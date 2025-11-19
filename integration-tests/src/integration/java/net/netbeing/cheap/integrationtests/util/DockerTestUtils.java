package net.netbeing.cheap.integrationtests.util;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

/**
 * Utility class for Docker-based integration tests.
 * Provides methods for waiting on container readiness and retrieving container information.
 */
@Slf4j
public class DockerTestUtils {

    private DockerTestUtils() {
        // Utility class
    }

    /**
     * Wait for a database container to be ready by checking its health status.
     * Does NOT attempt direct database connections.
     *
     * @param dockerClient Docker client instance
     * @param containerId Container ID or name
     * @param maxWaitSeconds Maximum time to wait in seconds
     * @return true if database is ready, false if timeout
     */
    public static boolean waitForDatabaseReady(DockerClient dockerClient, String containerId, int maxWaitSeconds) {
        log.info("Waiting for database container {} to be ready (max {} seconds)...", containerId, maxWaitSeconds);

        long startTime = System.currentTimeMillis();
        long maxWaitMillis = maxWaitSeconds * 1000L;
        int attempt = 0;

        while (System.currentTimeMillis() - startTime < maxWaitMillis) {
            attempt++;
            try {
                InspectContainerResponse inspection = dockerClient.inspectContainerCmd(containerId).exec();

                // Check if container is running
                if (inspection.getState() == null || !Boolean.TRUE.equals(inspection.getState().getRunning())) {
                    log.debug("Attempt {}: Container {} is not running yet", attempt, containerId);
                    Thread.sleep(1000);
                    continue;
                }

                // Check health status
                InspectContainerResponse.ContainerState.Health health = inspection.getState().getHealth();
                if (health != null) {
                    String healthStatus = health.getStatus();
                    log.debug("Attempt {}: Container {} health status: {}", attempt, containerId, healthStatus);

                    if ("healthy".equals(healthStatus)) {
                        log.info("Database container {} is ready after {} seconds",
                                containerId, (System.currentTimeMillis() - startTime) / 1000);
                        return true;
                    }
                } else {
                    // No health check defined, assume ready if running
                    log.debug("No health check defined for {}, assuming ready since container is running", containerId);
                    return true;
                }

                // Exponential backoff with cap
                long backoffMillis = Math.min(5000, 500 * (long) Math.pow(1.5, Math.min(attempt, 10)));
                Thread.sleep(backoffMillis);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for database container {}", containerId, e);
                return false;
            } catch (Exception e) {
                log.debug("Attempt {}: Error checking container {}: {}", attempt, containerId, e.getMessage());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        log.error("Database container {} did not become ready within {} seconds", containerId, maxWaitSeconds);
        return false;
    }

    /**
     * Wait for a REST service to be ready by polling its health endpoint.
     *
     * @param baseUrl Base URL of the REST service (e.g., "http://localhost:8080")
     * @param maxWaitSeconds Maximum time to wait in seconds
     * @return true if service is ready, false if timeout
     */
    public static boolean waitForRestServiceReady(String baseUrl, int maxWaitSeconds) {
        log.info("Waiting for REST service at {} to be ready (max {} seconds)...", baseUrl, maxWaitSeconds);

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();

        long startTime = System.currentTimeMillis();
        long maxWaitMillis = maxWaitSeconds * 1000L;
        int attempt = 0;

        while (System.currentTimeMillis() - startTime < maxWaitMillis) {
            attempt++;
            try {
                Boolean isHealthy = webClient.get()
                        .uri("/actuator/health")
                        .retrieve()
                        .onStatus(
                                status -> status != HttpStatus.OK,
                                response -> Mono.empty()
                        )
                        .bodyToMono(Map.class)
                        .map(body -> "UP".equals(body.get("status")))
                        .timeout(Duration.ofSeconds(3))
                        .block();

                if (Boolean.TRUE.equals(isHealthy)) {
                    log.info("REST service at {} is ready after {} seconds",
                            baseUrl, (System.currentTimeMillis() - startTime) / 1000);
                    return true;
                }

                log.debug("Attempt {}: Service at {} returned health status but not UP", attempt, baseUrl);

            } catch (Exception e) {
                log.debug("Attempt {}: Service at {} not ready yet: {}", attempt, baseUrl, e.getMessage());
            }

            // Exponential backoff with cap
            try {
                long backoffMillis = Math.min(5000, 500 * (long) Math.pow(1.5, Math.min(attempt, 10)));
                Thread.sleep(backoffMillis);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for REST service at {}", baseUrl, ie);
                return false;
            }
        }

        log.error("REST service at {} did not become ready within {} seconds", baseUrl, maxWaitSeconds);
        return false;
    }

    /**
     * Get the dynamically mapped host port for a container's exposed port.
     *
     * @param dockerClient Docker client instance
     * @param containerId Container ID or name
     * @param containerPort Port number exposed by the container
     * @return Host port number, or -1 if not found
     */
    public static int getDynamicPort(DockerClient dockerClient, String containerId, int containerPort) {
        try {
            InspectContainerResponse inspection = dockerClient.inspectContainerCmd(containerId).exec();

            if (inspection.getNetworkSettings() == null ||
                inspection.getNetworkSettings().getPorts() == null) {
                log.error("No network settings found for container {}", containerId);
                return -1;
            }

            ContainerPort[] ports = inspection.getNetworkSettings().getPorts().getBindings().entrySet().stream()
                    .filter(entry -> entry.getKey().getPort() == containerPort)
                    .flatMap(entry -> Arrays.stream(entry.getValue()))
                    .toArray(ContainerPort[]::new);

            if (ports.length > 0 && ports[0].getPublicPort() != null) {
                int hostPort = ports[0].getPublicPort();
                log.debug("Container {} port {} is mapped to host port {}", containerId, containerPort, hostPort);
                return hostPort;
            }

            log.error("No port mapping found for container {} port {}", containerId, containerPort);
            return -1;

        } catch (Exception e) {
            log.error("Error getting dynamic port for container {}", containerId, e);
            return -1;
        }
    }

    /**
     * Execute a command in a running container.
     * This is primarily for debugging purposes.
     *
     * @param dockerClient Docker client instance
     * @param containerId Container ID or name
     * @param command Command to execute (e.g., ["sh", "-c", "ls -la"])
     * @return Command output, or null if execution failed
     */
    public static String execInContainer(DockerClient dockerClient, String containerId, String... command) {
        log.debug("Executing command in container {}: {}", containerId, Arrays.toString(command));

        try {
            String execId = dockerClient.execCreateCmd(containerId)
                    .withCmd(command)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec()
                    .getId();

            StringBuilder output = new StringBuilder();
            dockerClient.execStartCmd(execId)
                    .exec(new com.github.dockerjava.api.async.ResultCallback.Adapter<>() {
                        @Override
                        public void onNext(com.github.dockerjava.api.model.Frame frame) {
                            output.append(new String(frame.getPayload()));
                        }
                    })
                    .awaitCompletion();

            String result = output.toString();
            log.debug("Command output: {}", result);
            return result;

        } catch (Exception e) {
            log.error("Error executing command in container {}", containerId, e);
            return null;
        }
    }
}
