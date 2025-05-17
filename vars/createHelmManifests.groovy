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
              sed -i "s|tag:.*|tag: ${env.BUILD_NUMBER}|" values.yaml
              sed -i 's/port: 80/port: 3000/' values.yaml
            else
              ENV="staging"
              sed -i "s|tag:.*|tag: ${env.BUILD_NUMBER}-${branchName}|" values.yaml
              sed -i 's/port: 80/port: 3000/' values.yaml
            fi
            
            # Ensure the environment directory exists
            mkdir -p \$ENV
            cd \$ENV
            
            # Create Helm chart if it doesn't exist
            cd ../../ 
            helm template "${serviceName}" -s templates/argocd-ingress.yaml > "${serviceName}"/\$ENV/argocd-ingress.yaml
            helm template "${serviceName}" -s templates/service.yaml > "${serviceName}"/\$ENV/service.yaml
            helm template "${serviceName}" -s templates/deployment.yaml > "${serviceName}"/\$ENV/deployment.yaml
          """
        } catch (Exception e) {
          currentBuild.result = 'FAILURE'
          throw e
        }
      }
    }
  }
}
      
