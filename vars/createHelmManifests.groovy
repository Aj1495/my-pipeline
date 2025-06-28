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
            echo "=== UPDATED SHARED LIBRARY CODE - JANUARY 2025 ==="
            echo "Creating Helm charts for ${serviceName}..."
            # Use the correct Helm chart path
            helm template "${serviceName}" k8s-mainfest/kubernetes-2025
          """
        } catch (Exception e) {
          currentBuild.result = 'FAILURE'
          throw e
        }
      }
    }
  }
}}
