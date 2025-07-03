def call(Map config) {
    def dockerOrg = config.dockerOrg
    def dockerCredentialsId = config.dockerCredentialsId
    def dockerTag

    if (config.applicationType == 'java') {
        stage('Generate Docker Tag from pom.xml') {
            echo "Generating Docker tag for Java app..."
            dockerTag = getJavaDockerTag(config)
            echo "Generated Docker tag: ${dockerTag}"
        }
    } else if (config.applicationType == 'nodejs') {
        stage('Generate Docker Tag from package.json') {
            dockerTag = getNodeDockerTag(config)
            echo "Generated Docker tag: ${dockerTag}"
        }
    } else if (config.applicationType == 'angular') {
        stage('Generate Docker Tag for Angular App') {
            dockerTag = "${env.BUILD_NUMBER}"
            echo "Generated Docker tag for Angular: ${dockerTag}"
        }
    } else {
        error "❌ Unsupported applicationType: ${config.applicationType}"
    }

    stage('Docker Build') {
        echo "Building Docker image: ${dockerOrg}/${JOB_NAME}:${dockerTag}..."
        sh "docker build -t ${dockerOrg}/${JOB_NAME}:${dockerTag} ."
        echo "Docker image built successfully."
    }

    stage('Containerization & Push') {
        echo "Logging into Docker Hub and pushing image..."
        withCredentials([usernamePassword(credentialsId: dockerCredentialsId, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
            sh "echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin"
            sh "docker push ${dockerOrg}/${JOB_NAME}:${dockerTag}"
            sh "docker rmi ${dockerOrg}/${JOB_NAME}:${dockerTag}"
        }
        echo "Docker image pushed and local image removed."
    }
}
