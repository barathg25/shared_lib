def call(Map pipelineConfig = [:]) {
    def defaultTag = env.BUILD_NUMBER ?: "latest"
    def files = sh(script: "ls package*.json 2>/dev/null || true", returnStdout: true).trim().split("\n")

    for (file in files) {
        try {
            def json = readJSON file: file
            if (json?.version) {
                env.DOCKER_TAG = json.version
                echo "✅ Docker tag found in ${file}: ${json.version}"
                return json.version
            }
        } catch (e) {
            echo "⚠️ Skipped ${file}: ${e.message}"
        }
    }

    echo "⚠️ No version found in package*.json, falling back to ${defaultTag}"
    env.DOCKER_TAG = defaultTag
    return defaultTag
}
