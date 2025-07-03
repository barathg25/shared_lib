def call(Map config) {
    def argoCdRepoUrl = config.argoCdRepoUrl
    def argoCdBranch = config.argoCdBranch
    def gitlabCredentialsId = config.gitlabCredentialsId
    def argoCdGitUserEmail = config.argoCdGitUserEmail
    def argoCdGitUserName = config.argoCdGitUserName
    def deploymentFilePath = config.deploymentFilePath
    def dockerOrg = config.dockerOrg
    def IMAGE_NAME = config.IMAGE_NAME
    def kubernetesNamespace = config.kubernetesNamespace
    
    stage('Update ArgoCD Git Repo') {
        echo "Cloning ArgoCD Git repository: '${argoCdRepoUrl}' on branch '${argoCdBranch}'..."

        dir('argo-cd-repo') {
            git branch: argoCdBranch, credentialsId: gitlabCredentialsId, url: "https://${argoCdRepoUrl}"

            withCredentials([usernamePassword(
                credentialsId: gitlabCredentialsId,
                usernameVariable: 'GIT_USER',
                passwordVariable: 'GIT_PASS'
            )]) {
                sh "git pull --rebase https://${GIT_USER}:${GIT_PASS}@${argoCdRepoUrl} ${argoCdBranch}"

                echo "Updating '${deploymentFilePath}' with image tag '${imageTag}'..."
                sh "sed -i 's#${dockerOrg}/${IMAGE_NAME}:[^ ]*#${dockerOrg}/${IMAGE_NAME}:${imageTag}#g' ${deploymentFilePath}"

                sh "git config user.email '${argoCdGitUserEmail}'"
                sh "git config user.name '${argoCdGitUserName}'"

                sh 'git add .'
                sh "git diff --cached --quiet || git commit -m \"Image tag ${imageTag} for ${JOB_NAME} updated by Jenkins\""
                sh "git push https://${GIT_USER}:${GIT_PASS}@${argoCdRepoUrl} ${argoCdBranch}"
            }
        }

        echo "ArgoCD Git repository updated with tag '${imageTag}'."
    }
    node(pipelineConfig.cdAgentLabel ?: 'cd') {
     stage('Deploy to Kubernetes') {
        echo "Deploying new image to Kubernetes namespace '${kubernetesNamespace}'..."
 

        sh """
            kubectl set image deployment/${JOB_NAME} ${JOB_NAME}=${dockerOrg}/${IMAGE_NAME}:${imageTag} -n ${kubernetesNamespace}
        """
        echo "Kubernetes deployment initiated with tag '${imageTag}'."
        }
    }

}

