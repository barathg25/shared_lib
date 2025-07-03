def call(Map config) {
    def gitCredentialsId = config.gitlabCredentialsId
    def gitUserEmail     = config.argoCdGitUserEmail
    def gitUserName      = config.argoCdGitUserName
    def pomFile          = config.pomFile ?: 'pom.xml'
    def repoUrl          = config.sourceRepoUrl
    def branch           = config.sourceBranch

    stage('Increment POM Version') {
        echo "🔍 Reading and incrementing version from ${pomFile}..."

        def pomText = readFile(pomFile)
        def originalVersion = new XmlSlurper(false, false).parseText(pomText).version.text()

        if (!originalVersion) {
            error "❌ No <version> tag found in ${pomFile}"
        }

        echo "📌 Original version: ${originalVersion}"

        // Split version and suffix
        def versionMain = originalVersion.tokenize('-')[0]
        def suffix = originalVersion.contains('-') ? '-' + originalVersion.split('-', 2)[1] : ''

        def versionParts = versionMain.tokenize('.')

        // Increment last numeric part
        def lastIndex = versionParts.size() - 1
        versionParts[lastIndex] = (versionParts[lastIndex] as Integer) + 1

        def newVersion = versionParts.join('.') + suffix
        echo "🚀 New version: ${newVersion}"

        // Update pom.xml
        sh "sed -i 's|<version>${originalVersion}</version>|<version>${newVersion}</version>|' ${pomFile}"

        def cleanedRepoUrl = repoUrl.replaceFirst(/^https?:\/\//, '')

        // Git Push
        withCredentials([usernamePassword(
            credentialsId: gitCredentialsId,
            usernameVariable: 'GIT_USER',
            passwordVariable: 'GIT_PASS'
        )]) {
            sh "git config user.email '${gitUserEmail}'"
            sh "git config user.name '${gitUserName}'"
            sh "git add ${pomFile}"
            sh "git diff --cached --quiet || git commit -m 'Bump version to ${newVersion} in pom.xml'"
            sh "git push https://${GIT_USER}:${GIT_PASS}@${cleanedRepoUrl} HEAD:refs/heads/${branch}"
        }

        echo "✅ pom.xml updated and pushed with version: ${newVersion}"
        env.DOCKER_TAG = newVersion
    }
}
