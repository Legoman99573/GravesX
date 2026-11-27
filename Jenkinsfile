pipeline {
    agent any
    tools { maven 'maven'; jdk 'jdk17' }

    stages {
        stage('Install bundled libs') {
            steps { sh 'bash build-libs.sh' }
        }
        stage('Build') {
            // one reactor build: common -> server -> proxy (core, velocity, bungeecord) -> dist
            steps { sh 'mvn -B -s jenkins-settings.xml clean package' }
        }
    }
    post {
        success {
            archiveArtifacts artifacts: 'dist/target/GravesX-*.jar, proxy/velocity/target/GravesX-Velocity-*.jar, proxy/bungeecord/target/GravesX-BungeeCord-*.jar', fingerprint: true
        }
    }
}
