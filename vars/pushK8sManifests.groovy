def call(String serviceName, String branchName) {
  stage('Push K8s Manifests') {
    ansiColor('xterm') {
      withCredentials([
        usernamePassword(
          credentialsId: '1360ab06-c1b5-4bc8-bc4d-89977f8400cf',
          usernameVariable: 'GIT_USER',
          passwordVariable: 'GIT_TOKEN'
        )
      ]) {
        try {
          sh """ 
            echo "Preparing to push Helm/K8s Manifests to separate repo ..."
            
            # Navigate to the k8s manifests repo directory
            cd k8s-manifests-repo
            
            # Configure git if not already configured
            git config user.email skswami91@gmail.com
            git config user.name skswami91
            

            git add .
            git commit -m "Updating manifests for ${serviceName} - build #${env.BUILD_NUMBER}"
            git push origin main --force

          """
        } catch (Exception e) {
          currentBuild.result = 'FAILURE'
          throw e
        }
      }
    }
  }
}
