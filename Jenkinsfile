pipeline {
    agent any
    tools { maven 'maven'; jdk 'jdk17' }   // jdk must be >= java.version in the root pom.xml

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
