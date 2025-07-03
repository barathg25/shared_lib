// vars/mavenBuild.groovy
// This file contains the logic for performing a Maven build.

// The 'call' method allows this script to be invoked directly from a Jenkinsfile
// or another Groovy script within the shared library.
// It accepts a 'config' map, which should contain resolved parameters.
def call(Map config) {
    // Retrieve parameters needed for the Maven build from the config map.
    // 'skipDependencyCheck' determines whether to skip the dependency-check plugin.
    def skipDependencyCheck = config.skipDependencyCheck

    stage('Maven Build') {
        echo "Running Maven build..."
        def mavenCommand = "mvn clean package"
        // Conditionally add the dependency-check skip flag based on the parameter.
        if (skipDependencyCheck) {
            mavenCommand += " -Ddependency-check.skip=true"
        }
        sh mavenCommand // Execute the Maven command.
        echo "Maven build completed."
    }
}
