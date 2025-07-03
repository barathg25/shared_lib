def call(Map pipelineConfig = [:]) {
    def pomFile = pipelineConfig.pomFile ?: 'pom.xml'

    // Use Jenkins `readFile` to safely read from workspace
    def pomText = readFile(pomFile)
    def pom = new XmlSlurper().parseText(pomText)

    def version = pom.version?.text()
    if (!version) {
        error "ERROR: <version> tag not found in ${pomFile}"
    }

    env.DOCKER_TAG = version
    echo "Generated Docker Tag from POM: ${version}"
    return version
}
