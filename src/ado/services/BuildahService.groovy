package ado.services

import ado.core.BaseService
import ado.core.Config
import ado.clients.BuildahClient
import ado.models.ContainerImage

class BuildahService extends BaseService {
    private BuildahClient client
    
    BuildahService(script, Config config = null) {
        super(script, config)
        this.client = new BuildahClient(script, config)
    }
    
    @Override
    void initialize() {
        logger.info("Initializing Buildah Service")
        client.validateBuildahInstallation()
    }
    
    @Override
    boolean validate() {
        return client.isBuildahAvailable()
    }
    
    /**
     * Build container image using buildah
     */
    ContainerImage build(Map params) {
        return executeWithErrorHandling {
            logger.info("Building container image: ${params.imageName}")
            
            def image = new ContainerImage(
                name: params.imageName,
                tag: params.imageTag ?: script.env.BUILD_NUMBER,
                dockerfile: params.dockerfile ?: 'Containerfile',
                context: params.context ?: '.'
            )
            
            client.build(image, params)
            
            return image
        }
    }
    
    /**
     * Push container image to registry
     */
    void push(ContainerImage image, Map params = [:]) {
        executeWithErrorHandling {
            logger.info("Pushing container image: ${image.fullName}")
            
            def registry = params.registry ?: config.get('buildah.registry')
            def credentialsId = params.credentialsId ?: config.get('buildah.credentialsId')
            
            client.push(image, registry, credentialsId)
        }
    }
    
    /**
     * Scan container image for vulnerabilities
     */
    Map scan(ContainerImage image, Map params = [:]) {
        return executeWithErrorHandling {
            logger.info("Scanning container image: ${image.fullName}")
            
            def tool = params.tool ?: 'trivy'
            def severity = params.severity ?: 'HIGH,CRITICAL'
            
            return client.scan(image, tool, severity)
        }
    }
    
    /**
     * Build multi-architecture images
     */
    ContainerImage buildMultiArch(Map params) {
        return executeWithErrorHandling {
            logger.info("Building multi-arch image: ${params.imageName}")
            
            def image = new ContainerImage(
                name: params.imageName,
                tag: params.imageTag ?: script.env.BUILD_NUMBER
            )
            
            def platforms = params.platforms ?: ['linux/amd64', 'linux/arm64']
            
            client.buildMultiArch(image, platforms, params)
            
            return image
        }
    }
    
    /**
     * Run container for testing
     */
    String runContainer(ContainerImage image, Map params = [:]) {
        return executeWithErrorHandling {
            logger.info("Running container from image: ${image.fullName}")
            return client.runContainer(image, params)
        }
    }
    
    /**
     * Stop and remove container
     */
    void stopContainer(String containerName) {
        executeWithErrorHandling {
            logger.info("Stopping container: ${containerName}")
            client.stopContainer(containerName)
        }
    }
    
    /**
     * Tag image
     */
    void tag(ContainerImage image, String newTag) {
        executeWithErrorHandling {
            logger.info("Tagging image ${image.fullName} as ${image.name}:${newTag}")
            client.tag(image, newTag)
        }
    }
    
    /**
     * Pull image from registry
     */
    void pull(String imageName, Map params = [:]) {
        executeWithErrorHandling {
            logger.info("Pulling image: ${imageName}")
            
            def registry = params.registry ?: config.get('buildah.registry')
            def credentialsId = params.credentialsId ?: config.get('buildah.credentialsId')
            
            client.pull(imageName, registry, credentialsId)
        }
    }
    
    /**
     * List images
     */
    List<Map> listImages(Map params = [:]) {
        return executeWithErrorHandling {
            logger.info("Listing container images")
            return client.listImages(params)
        }
    }
    
    /**
     * Remove image
     */
    void removeImage(String imageName, Map params = [:]) {
        executeWithErrorHandling {
            logger.info("Removing image: ${imageName}")
            client.removeImage(imageName, params)
        }
    }
    
    @Override
    void cleanup() {
        super.cleanup()
        client.cleanup()
    }
}
