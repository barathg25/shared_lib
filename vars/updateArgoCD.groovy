// vars/updateArgoCD.groovy
// This file manages updating the build version in an ArgoCD Git repository for GitOps.

// The 'call' method allows this script to be invoked directly.
// It accepts a 'config' map containing relevant parameters.
def call(Map config) {
    def argoCdRepoUrl = config.argoCdRepoUrl
    def argoCdBranch = config.argoCdBranch
    def gitlabCredentialsId = config.gitlabCredentialsId
    def argoCdGitUserEmail = config.argoCdGitUserEmail
    def argoCdGitUserName = config.argoCdGitUserName
    def deploymentFilePath = config.deploymentFilePath
    def dockerOrg = config.dockerOrg

    stage('Update ArgoCD Git Repo') {
        echo "Cloning ArgoCD Git repository: '${argoCdRepoUrl}' on branch '${argoCdBranch}'..."

        def imageTag = env.DOCKER_TAG
        if (!imageTag) {
            error "DOCKER_TAG not defined. Please make sure 'generateDockerTag' was called before this stage."
        }

        dir('argo-cd-repo') {
            git branch: argoCdBranch, credentialsId: gitlabCredentialsId, url: "https://${argoCdRepoUrl}"

            withCredentials([usernamePassword(
                credentialsId: gitlabCredentialsId,
                usernameVariable: 'GIT_USER',
                passwordVariable: 'GIT_PASS'
            )]) {
                sh "git pull --rebase https://${GIT_USER}:${GIT_PASS}@${argoCdRepoUrl} ${argoCdBranch}"

                echo "Updating '${deploymentFilePath}' with image tag '${imageTag}'..."
                sh "sed -i 's#${dockerOrg}/${JOB_NAME}:[^ ]*#${dockerOrg}/${JOB_NAME}:${imageTag}#g' ${deploymentFilePath}"

                sh "git config user.email '${argoCdGitUserEmail}'"
                sh "git config user.name '${argoCdGitUserName}'"

                sh 'git add .'
                sh "git diff --cached --quiet || git commit -m \"Image tag ${imageTag} for ${JOB_NAME} updated by Jenkins\""
                sh "git push https://${GIT_USER}:${GIT_PASS}@${argoCdRepoUrl} ${argoCdBranch}"
            }
        }

        echo "ArgoCD Git repository updated with tag '${imageTag}'."
    }
}
