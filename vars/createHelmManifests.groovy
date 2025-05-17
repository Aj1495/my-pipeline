def call(String serviceName, String branchName) {
  stage('Create Helm Manifests') {
    ansiColor('xterm') {
      withCredentials([
        usernamePassword(
          credentialsId: 'dockerCreds',
          usernameVariable: 'DOCKER_USER',
          passwordVariable: 'DOCKER_PASS'
        ),
        usernamePassword(
          credentialsId: 'alb-dns-name',
          usernameVariable: 'ALB',
          passwordVariable: 'ALB_DNS'
        )
      ]) {
        try {
          sh """ 
            echo "Creating Helm charts for ${serviceName}..."
            
            # Navigate to the k8s manifests repo directory
            cd k8s-manifests-repo/${serviceName}
            
            # Determine environment based on branch
            if [ "${branchName}" = "main" ]; then
              ENV="production"
            else
              ENV="staging"
            fi
            
            # Ensure the environment directory exists
            mkdir -p \$ENV
            cd \$ENV
            
            # Create Helm chart if it doesn't exist
            ls -ltrh ../
            ls -ltrh ../../ 

          """
        } catch (Exception e) {
          currentBuild.result = 'FAILURE'
          throw e
        }
      }
    }
  }
}
      
