// vars/modifyEnvFile.groovy
def call(Map pipelineConfig) {
    // Validate required inputs
    if (!pipelineConfig.envReplaceFrom || !pipelineConfig.envReplaceTo) {
        error "ERROR: 'envReplaceFrom' and 'envReplaceTo' must be provided in pipelineConfig."
    }

    def replaceFrom = pipelineConfig.envReplaceFrom
    def replaceTo = pipelineConfig.envReplaceTo

    echo "--- Modifying .env file ---"
    echo "Replacing '${replaceFrom}' with '${replaceTo}'"
    sh "sed -i 's/${replaceFrom}/${replaceTo}/g' .env"
}
