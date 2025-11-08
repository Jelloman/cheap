// Root project build configuration

// Create a verification task to run integration tests
tasks.register("integration-tests") {
    group = "verification"
    description = "Runs integration tests"

    dependsOn(":integration-tests:integrationTest")

    doLast {
        println("Integration tests completed")
    }
}

// Exclude integration-tests from the default build
// Only run integration-tests tasks when explicitly requested
gradle.taskGraph.whenReady {
    val hasIntegrationTestsInRequest = gradle.startParameter.taskNames.any { taskName ->
        taskName.contains("integration-tests") || taskName.contains(":integration-tests:")
    }

    if (!hasIntegrationTestsInRequest) {
        allTasks.forEach { task ->
            if (task.project.path == ":integration-tests") {
                task.enabled = false
            }
        }
    }
}

