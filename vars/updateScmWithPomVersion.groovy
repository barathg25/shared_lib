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

        // Increment patch version
        def parts = originalVersion.tokenize('.')
        def patchRaw = parts[2]
        def patch = patchRaw.replaceAll(/\D.*/, '').toInteger() + 1
        def suffix = patchRaw -~ /^\d+/  // Keep suffix like -SNAPSHOT
        def newVersion = "${parts[0]}.${parts[1]}.${patch}${suffix}"

        echo "🚀 New version: ${newVersion}"

        // Update pom.xml
        sh "sed -i 's|<version>${originalVersion}</version>|<version>${newVersion}</version>|' ${pomFile}"

        def cleanedRepoUrl = repoUrl.replaceFirst(/^https?:\/\//, '')

        // Push changes to Git
        withCredentials([usernamePassword(
            credentialsId: gitCredentialsId,
            usernameVariable: 'GIT_USER',
            passwordVariable: 'GIT_PASS'
        )]) {
            // sh "git pull --rebase https://${GIT_USER}:${GIT_PASS}@${cleanedRepoUrl} ${branch}"
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
