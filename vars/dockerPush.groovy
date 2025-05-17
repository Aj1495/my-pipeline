def call(String serviceName, String branchName) {
  stage('Docker Push') {
    ansiColor('xterm') {
      withCredentials([
        usernamePassword(
          credentialsId: 'dockerCreds',
          usernameVariable: 'DOCKER_USER',
          passwordVariable: 'DOCKER_PASS'
        )
      ]) {
        try {
          sh """ 
          echo "Logging into Docker Registry for push..."
          docker login -u \$DOCKER_USER -p \$DOCKER_PASS
          """
          
          if (branchName in ["main", "staging", "preprod", "production", "master"]) {
            def imageTag = "${DOCKER_USER}/${serviceName}:${env.BUILD_NUMBER}"
            sh """
            echo "Tagging image as ${imageTag}"
            docker tag ${serviceName}:${env.BUILD_NUMBER} ${imageTag}

            echo "Pushing Docker image: ${imageTag}"
            docker push ${imageTag}
            """
          } else {
            def imageTag = "${DOCKER_USER}/${serviceName}:${env.BUILD_NUMBER}-${branchName}"
            sh """
            echo "Tagging image as ${imageTag}"
            docker tag ${serviceName}:${env.BUILD_NUMBER}-${branchName} ${imageTag}

            echo "Pushing Docker image: ${imageTag}"
            docker push ${imageTag}
            """
          }
        } catch (Exception e) {
          currentBuild.result = 'FAILURE'
          throw e
        }
      }
    }
  }
}
