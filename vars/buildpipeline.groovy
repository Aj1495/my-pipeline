def call(String masterBuild) {
  def git_app_repo = scm.userRemoteConfigs[0].url
  def SERVICE_NAME = scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
  def git_app_branch = "main"

  podTemplate(
    label: 'jenkins-agent',
    containers: [
      containerTemplate(
        name: 'docker',
        image: 'docker:20.10.8',
        command: 'cat',
        ttyEnabled: true,
        envVars: [
          envVar(key: 'DOCKER_HOST', value: 'tcp://localhost:2375'),
          envVar(key: 'DOCKER_TLS_CERTDIR', value: '')
        ],
        resourceRequestCpu: '300m',
        resourceRequestMemory: '2Gi',
        resourceLimitCpu: '1',
        resourceLimitMemory: '3Gi'
      ),
      containerTemplate(
        name: 'helm',
        image: 'alpine/helm:3.13.0',
        command: 'cat',
        ttyEnabled: true,
        resourceRequestCpu: '50m',
        resourceRequestMemory: '128Mi',
        resourceLimitCpu: '100m',
        resourceLimitMemory: '256Mi'
      ),
      containerTemplate(
        name: 'dind-daemon',
        image: 'docker:20.10.8-dind',
        privileged: true,
        args: '--host tcp://0.0.0.0:2375 --host unix:///var/run/docker.sock',
        envVars: [
          envVar(key: 'DOCKER_TLS_CERTDIR', value: '')
        ],
        resourceRequestCpu: '200m',
        resourceRequestMemory: '512Mi',
        resourceLimitCpu: '500m',
        resourceLimitMemory: '1Gi'
      )
    ],
    volumes: [
      emptyDirVolume(mountPath: '/var/lib/docker', memory: false)
    ]
  ) {
    node('jenkins-agent') {
      properties([
        buildDiscarder(logRotator(numToKeepStr: '5')),
        disableConcurrentBuilds(),
      ])

      stage('CleanWorkspace') {
        sh 'whoami && pwd'
      }

      stage('Checkout Code') {
        checkout([
          $class: 'GitSCM',
          branches: [[name: "*/${git_app_branch}"]],
          doGenerateSubmoduleConfigurations: false,
          extensions: [
            [$class: 'LocalBranch', localBranch: "${git_app_branch}"]
          ],
          submoduleCfg: [],
          userRemoteConfigs: [
            [credentialsId: '1360ab06-c1b5-4bc8-bc4d-89977f8400cf', url: "${git_app_repo}"]
          ]
        ])
      }

      stage('Build Docker Image') {
        container('docker') {
          // Wait for Docker daemon to be ready
          sh '''
            echo "Waiting for Docker daemon to be ready..."
            i=1
            while [ $i -le 30 ]; do
              if docker info > /dev/null 2>&1; then
                echo "Docker daemon is ready!"
                break
              fi
              echo "Docker daemon not ready yet... retrying in 3s"
              sleep 3
              if [ $i -eq 30 ]; then
                echo "Docker daemon failed to start after 90 seconds"
                exit 1
              fi
              i=$((i+1))
            done
          '''
          // Docker build command (customize as needed)
          sh "docker build -t ${SERVICE_NAME}:${git_app_branch} ./backend"
        }
      }

      stage('Push Docker Image') {
        container('docker') {
          // Login to Docker Hub using Jenkins credentials
          withCredentials([usernamePassword(credentialsId: 'dockerCreds', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
            sh 'echo $DOCKER_PASSWORD | docker login -u $DOCKER_USERNAME --password-stdin'
          }

          // Tag with your Docker Hub username
          sh "docker tag ${SERVICE_NAME}:${git_app_branch} bobthe1495/${SERVICE_NAME}:${git_app_branch}"

          // Docker push command
          sh "docker push bobthe1495/${SERVICE_NAME}:${git_app_branch}"
        }
      }

      stage('Pull Kubernetes Manifests') {
        echo "Pulling Helm charts for ${SERVICE_NAME}:${git_app_branch}"
      }

      stage('Clone K8s Manifests Repo') {
        sh '''
          rm -rf k8s-mainfest
          git clone https://github.com/Aj1495/k8smain.git k8s-mainfest
        '''
      }

      stage('Helm Create Manifests') {
        container('helm') {
          sh "helm template ${SERVICE_NAME} k8s-mainfest/kubernetes-2025"
        }
      }

      stage('Push Kubernetes Manifests') {
        echo "Pushing Kubernetes manifests for ${SERVICE_NAME}:${git_app_branch}"
      }
    }
  }
}
