package com.example.client.docker

import com.example.utils.Helper

/**
 * Docker Client - Handles Docker CLI operations
 */
class DockerClient {
    def shell
    def logger
    
    DockerClient(shell, logger = null) {
        this.shell = shell
        this.logger = logger ?: new Helper().getLogger()
    }
    
    /**
     * Build Docker image
     * @param dockerfile Path to Dockerfile
     * @param tag Image tag (e.g., myapp:1.0.0)
     * @param buildArgs Additional build arguments
     * @return Command output
     */
    def buildImage(String dockerfile, String tag, Map buildArgs = [:]) {
        def buildArgsStr = buildArgs.collect { k, v -> "--build-arg ${k}=${v}" }.join(' ')
        def cmd = "docker build -f ${dockerfile} -t ${tag} ${buildArgsStr} ."
        logger?.info("Building Docker image: ${cmd}")
        return shell.sh(script: cmd, returnStdout: true).trim()
    }
    
    /**
     * Push Docker image to registry
     * @param tag Image tag
     * @param registry Docker registry URL
     * @return Command output
     */
    def pushImage(String tag, String registry = '') {
        def fullTag = registry ? "${registry}/${tag}" : tag
        def tagCmd = "docker tag ${tag} ${fullTag}"
        def pushCmd = "docker push ${fullTag}"
        
        logger?.info("Pushing Docker image: ${fullTag}")
        shell.sh(script: tagCmd)
        return shell.sh(script: pushCmd, returnStdout: true).trim()
    }
    
    /**
     * Pull Docker image from registry
     * @param tag Image tag with registry
     * @return Command output
     */
    def pullImage(String tag) {
        def cmd = "docker pull ${tag}"
        logger?.info("Pulling Docker image: ${tag}")
        return shell.sh(script: cmd, returnStdout: true).trim()
    }
    
    /**
     * Get Docker image ID
     * @param tag Image tag
     * @return Image ID
     */
    def getImageId(String tag) {
        def cmd = "docker images -q ${tag}"
        logger?.info("Getting image ID for: ${tag}")
        return shell.sh(script: cmd, returnStdout: true).trim()
    }
    
    /**
     * Remove Docker image
     * @param tag Image tag
     * @param force Force removal
     * @return Command output
     */
    def removeImage(String tag, boolean force = false) {
        def forceFlag = force ? '-f' : ''
        def cmd = "docker rmi ${forceFlag} ${tag}"
        logger?.info("Removing Docker image: ${tag}")
        return shell.sh(script: cmd, returnStdout: true).trim()
    }
    
    /**
     * Login to Docker registry
     * @param registry Registry URL
     * @param username Username
     * @param password Password/Token
     * @return Success status
     */
    def login(String registry, String username, String password) {
        def cmd = "echo '${password}' | docker login -u ${username} --password-stdin ${registry}"
        logger?.info("Logging in to Docker registry: ${registry}")
        return shell.sh(script: cmd, returnStdout: true).trim()
    }
}