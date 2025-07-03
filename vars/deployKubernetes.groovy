// vars/deployKubernetes.groovy
// This file handles deploying the new Docker image to Kubernetes.

// The 'call' method allows this script to be invoked directly.
// It accepts a 'config' map containing relevant parameters.
def call(Map config) {
    def kubernetesNamespace = config.kubernetesNamespace
    def dockerOrg = config.dockerOrg

    stage('Deploy to Kubernetes') {
        echo "Deploying new image to Kubernetes namespace '${kubernetesNamespace}'..."

        def imageTag = env.DOCKER_TAG
        if (!imageTag) {
            error "DOCKER_TAG not defined. Please make sure 'generateDockerTag' was called before this stage."
        }

        sh """
            kubectl set image deployment/${JOB_NAME} ${JOB_NAME}=${dockerOrg}/${JOB_NAME}:${imageTag} -n ${kubernetesNamespace}
        """
        echo "Kubernetes deployment initiated with tag '${imageTag}'."
    }
}

