package net.netbeing.cheap.integrationtests.util;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HealthCheck;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.PruneType;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import net.netbeing.cheap.util.CheapException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages Docker container lifecycle for integration tests.
 * Provides a fluent API for container configuration and handles cleanup on test completion.
 */
@Slf4j
public class DockerContainerManager implements AutoCloseable
{
    private final DockerClient dockerClient;
    private final List<String> managedContainers;
    private final List<String> managedNetworks;
    private final boolean logContainerOutput;

    /**
     * Create a new DockerContainerManager.
     *
     * @param logContainerOutput Whether to log container output for debugging
     */
    public DockerContainerManager(boolean logContainerOutput)
    {
        this.logContainerOutput = logContainerOutput;
        this.managedContainers = new ArrayList<>();
        this.managedNetworks = new ArrayList<>();

        // Create Docker client
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .maxConnections(100)
            .connectionTimeout(Duration.ofSeconds(30))
            .responseTimeout(Duration.ofSeconds(45))
            .build();

        this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
        log.info("DockerContainerManager initialized");
    }

    /**
     * Get the Docker client instance.
     *
     * @return Docker client
     */
    public DockerClient getDockerClient()
    {
        return dockerClient;
    }

    /**
     * Create a Docker network for container communication.
     *
     * @param networkName Network name
     * @return Network ID
     */
    @SuppressWarnings("UnusedReturnValue")
    public String createNetwork(String networkName)
    {
        log.info("Creating Docker network: {}", networkName);

        try {
            String networkId = dockerClient.createNetworkCmd()
                .withName(networkName)
                .withDriver("bridge")
                .exec()
                .getId();

            managedNetworks.add(networkId);
            log.info("Created network {} (ID: {})", networkName, networkId);
            return networkId;

        } catch (Exception e) {
            log.error("Error creating network {}", networkName, e);
            throw new CheapException("Failed to create network: " + networkName, e);
        }
    }

    /**
     * Start creating a container with a fluent builder API.
     *
     * @param imageName Docker image name
     * @return ContainerBuilder for fluent configuration
     */
    public ContainerBuilder container(String imageName)
    {
        return new ContainerBuilder(imageName);
    }

    /**
     * Start a container by ID.
     *
     * @param containerId Container ID
     */
    public void startContainer(String containerId)
    {
        log.info("Starting container: {}", containerId);

        try {
            dockerClient.startContainerCmd(containerId).exec();

            if (logContainerOutput) {
                attachLogger(containerId);
            }

            log.info("Started container: {}", containerId);

        } catch (Exception e) {
            log.error("Error starting container {}", containerId, e);
            throw new CheapException("Failed to start container: " + containerId, e);
        }
    }

    /**
     * Stop a container by ID.
     *
     * @param containerId Container ID
     */
    public void stopContainer(String containerId)
    {
        log.info("Stopping container: {}", containerId);

        try {
            dockerClient.stopContainerCmd(containerId)
                .withTimeout(10)
                .exec();

            log.info("Stopped container: {}", containerId);

        } catch (Exception e) {
            log.error("Error stopping container {}", containerId, e);
            // Don't throw - best effort cleanup
        }
    }

    /**
     * Remove a container by ID.
     *
     * @param containerId Container ID
     */
    public void removeContainer(String containerId)
    {
        log.info("Removing container: {}", containerId);

        try {
            dockerClient.removeContainerCmd(containerId)
                .withForce(true)
                .withRemoveVolumes(true)
                .exec();

            managedContainers.remove(containerId);
            log.info("Removed container: {}", containerId);

        } catch (Exception e) {
            log.error("Error removing container {}", containerId, e);
            // Don't throw - best effort cleanup
        }
    }

    /**
     * Attach a log callback to a container to capture its output.
     *
     * @param containerId Container ID
     */
    private void attachLogger(String containerId)
    {
        try {
            dockerClient.logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(true)
                .withSince(0)
                .exec(new ResultCallback.Adapter<Frame>()
                {
                    @Override
                    public void onNext(Frame frame)
                    {
                        String message = new String(frame.getPayload()).trim();
                        if (!message.isEmpty()) {
                            log.debug("[Container {}] {}", containerId.substring(0, 12), message);
                        }
                    }
                });
        } catch (Exception e) {
            log.warn("Failed to attach logger to container {}", containerId, e);
        }
    }

    /**
     * Clean up all managed containers and networks.
     */
    @Override
    public void close()
    {
        log.info("Cleaning up DockerContainerManager resources...");

        // Stop and remove all managed containers
        for (String containerId : new ArrayList<>(managedContainers)) {
            stopContainer(containerId);
            removeContainer(containerId);
        }

        // Remove all managed networks
        for (String networkId : new ArrayList<>(managedNetworks)) {
            try {
                dockerClient.removeNetworkCmd(networkId).exec();
                log.info("Removed network: {}", networkId);
            } catch (Exception e) {
                log.error("Error removing network {}", networkId, e);
            }
        }

        try {
            dockerClient.close();
        } catch (Exception e) {
            log.error("Error closing Docker client", e);
        }

        log.info("DockerContainerManager cleanup complete");
    }

    /**
     * Fluent builder for container configuration.
     */
    public class ContainerBuilder
    {
        private final String imageName;
        private final Map<String, String> environment = new HashMap<>();
        private final List<String> portBindings = new ArrayList<>();
        private final Map<String, String> volumeBinds = new HashMap<>();
        private String containerName;
        private String networkName;
        private HealthCheck healthCheck;

        private ContainerBuilder(String imageName)
        {
            this.imageName = imageName;
        }

        /**
         * Set the container name.
         *
         * @param name Container name
         * @return This builder
         */
        public ContainerBuilder name(String name)
        {
            this.containerName = name;
            return this;
        }

        /**
         * Add an environment variable.
         *
         * @param key   Environment variable name
         * @param value Environment variable value
         * @return This builder
         */
        public ContainerBuilder env(String key, String value)
        {
            this.environment.put(key, value);
            return this;
        }

        /**
         * Expose a port with dynamic host port mapping.
         *
         * @param containerPort Port to expose
         * @return This builder
         */
        public ContainerBuilder exposePort(int containerPort)
        {
            this.portBindings.add(String.valueOf(containerPort));
            return this;
        }

        /**
         * Bind a port to a specific host port.
         *
         * @param hostPort      Host port
         * @param containerPort Container port
         * @return This builder
         */
        @SuppressWarnings("unused")
        public ContainerBuilder bindPort(int hostPort, int containerPort)
        {
            this.portBindings.add(hostPort + ":" + containerPort);
            return this;
        }

        /**
         * Attach to a Docker network.
         *
         * @param network Network name
         * @return This builder
         */
        public ContainerBuilder network(String network)
        {
            this.networkName = network;
            return this;
        }

        /**
         * Mount a volume.
         *
         * @param hostPath      Host path
         * @param containerPath Container path
         * @return This builder
         */
        public ContainerBuilder volume(String hostPath, String containerPath)
        {
            this.volumeBinds.put(hostPath, containerPath);
            return this;
        }

        /**
         * Set a health check command.
         *
         * @param cmd Health check command (e.g., ["CMD-SHELL", "pg_isready"])
         * @return This builder
         */
        public ContainerBuilder healthCheck(String... cmd)
        {
            this.healthCheck = new HealthCheck()
                .withTest(Arrays.asList(cmd))
                .withInterval(5000000000L) // 5 seconds
                .withTimeout(3000000000L) // 3 seconds
                .withRetries(5);
            return this;
        }

        /**
         * Create and register the container.
         *
         * @return Container ID
         */
        public String create()
        {
            log.info("Creating container from image: {}", imageName);

            try {
                // Clean up any existing container with the same name from previous failed runs
                if (containerName != null) {
                    try {
                        dockerClient.removeContainerCmd(containerName)
                            .withForce(true)
                            .withRemoveVolumes(true)
                            .exec();
                        log.info("Removed existing container with name: {}", containerName);
                    } catch (Exception e) {
                        // Container doesn't exist, which is fine
                        log.debug("No existing container found with name: {}", containerName);
                    }
                }

                CreateContainerCmd cmd = dockerClient.createContainerCmd(imageName);

                if (containerName != null) {
                    cmd.withName(containerName);
                }

                if (!environment.isEmpty()) {
                    List<String> envList = new ArrayList<>();
                    environment.forEach((k, v) -> envList.add(k + "=" + v));
                    cmd.withEnv(envList);
                }

                HostConfig hostConfig = new HostConfig();

                if (!portBindings.isEmpty()) {
                    Ports ports = new Ports();
                    for (String binding : portBindings) {
                        String[] parts = binding.split(":");
                        int containerPort = parts.length == 2 ? Integer.parseInt(parts[1]) : Integer.parseInt(parts[0]);
                        ExposedPort exposedPort = ExposedPort.tcp(containerPort);

                        if (parts.length == 2) {
                            ports.bind(exposedPort, Ports.Binding.bindPort(Integer.parseInt(parts[0])));
                        } else {
                            ports.bind(exposedPort, Ports.Binding.empty());
                        }
                    }
                    hostConfig.withPortBindings(ports);
                }

                if (networkName != null) {
                    hostConfig.withNetworkMode(networkName);
                }

                if (!volumeBinds.isEmpty()) {
                    List<Bind> binds = new ArrayList<>();
                    volumeBinds.forEach((host, container) ->
                        binds.add(new Bind(host, new Volume(container)))
                    );
                    hostConfig.withBinds(binds);
                }

                cmd.withHostConfig(hostConfig);

                if (healthCheck != null) {
                    cmd.withHealthcheck(healthCheck);
                }

                CreateContainerResponse response = cmd.exec();
                String containerId = response.getId();

                managedContainers.add(containerId);
                log.info("Created container {} (ID: {})", containerName != null ? containerName : imageName,
                    containerId);

                return containerId;

            } catch (Exception e) {
                log.error("Error creating container from image {}", imageName, e);
                throw new CheapException("Failed to create container: " + imageName, e);
            }
        }

        /**
         * Create and start the container.
         *
         * @return Container ID
         */
        public String start()
        {
            String containerId = create();
            startContainer(containerId);
            return containerId;
        }
    }
}
