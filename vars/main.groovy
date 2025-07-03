def call(Map pipelineConfig) {
    // --- Validate required parameters ---
    if (!pipelineConfig.kubernetesNamespace) {
        error "ERROR: 'kubernetesNamespace' is required but not provided."
    }

    echo "--- Starting CI/CD Pipeline with Provided Config ---"
    echo "----------------------------------------------------"

    node(pipelineConfig.cicdAgentLabel ?: 'cicd') {
        stage('Cloning Source Code') {
        echo "Cloning source code..."
        checkout scm
        }
        if (pipelineConfig.applicationType == 'java') {
            echo "applicationtype = java"
            mavenBuild.call(pipelineConfig)
            dockerTagGeneration.call(pipelineConfig)
            updateScmWithPomVersion.call(pipelineConfig)

        } else if (pipelineConfig.applicationType == 'nodejs') {
            echo "applicationtype = nodejs"
            stage('modifyEnvFile') {
            modifyEnvFile.call(pipelineConfig)
            }
            dockerTagGeneration.call(pipelineConfig) // fallback for NodeJS

        } else if (pipelineConfig.applicationType == 'angular') {
            echo "applicationtype = angular"
            dockerTagGeneration.call(pipelineConfig) // fallback for Angular
            
        } else {
            error "Unsupported applicationType: '${pipelineConfig.applicationType}'."
        }

        updateArgoCD.call(pipelineConfig)

        cleanWs()
        echo "Workspace cleaned."
    }

    node(pipelineConfig.cdAgentLabel ?: 'cd') {
        deployKubernetes.call(pipelineConfig)
    }

    echo "--- CI/CD Pipeline Finished Successfully ---"
}
