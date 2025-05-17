def call(String serviceName, String branchName) {
  stage('Pull K8s Manifests') {
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
            echo "Preparing to pull Helm/K8s Manifests from repo ..."

            rm -rf k8s-manifests-repo
            mkdir k8s-manifests-repo
            cd k8s-manifests-repo
            git init
            git remote add origin https://\$GIT_USER:\$GIT_TOKEN@github.com/skswami91/k8s-manifests-2025.git
            git pull origin main
            
            # Create directory for service if it doesn't exist
            mkdir -p ${serviceName}
            cd ${serviceName}
            
            # Create environment-specific directory based on branch
            if [ "${branchName}" = "main" ]; then
              mkdir -p production
            else
              mkdir -p staging
            fi
          """
        } catch (Exception e) {
          currentBuild.result = 'FAILURE'
          throw e
        }
      }
    }
  }
}
