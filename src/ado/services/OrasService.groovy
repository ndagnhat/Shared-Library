package ado.services

import ado.core.BaseService
import ado.core.Config
import ado.clients.OrasClient

class OrasService extends BaseService {
    private OrasClient client
    
    OrasService(script, Config config = null) {
        super(script, config)
        this.client = new OrasClient(script, config)
    }
    
    @Override
    void initialize() {
        logger.info("Initializing ORAS Service")
        client.validateOrasInstallation()
    }
    
    @Override
    boolean validate() {
        return client.isOrasAvailable()
    }
    
    /**
     * Push file(s) to OCI registry (Nexus)
     */
    Map push(Map params) {
        return executeWithErrorHandling {
            logger.info("Pushing files to registry: ${params.reference}")
            
            def reference = params.reference
            def files = params.files ?: []
            def annotations = params.annotations ?: [:]
            def config = params.config
            def mediaType = params.mediaType
            def artifactType = params.artifactType
            
            return client.push(
                reference: reference,
                files: files,
                annotations: annotations,
                config: config,
                mediaType: mediaType,
                artifactType: artifactType
            )
        }
    }
    
    /**
     * Pull file(s) from OCI registry
     */
    Map pull(Map params) {
        return executeWithErrorHandling {
            logger.info("Pulling files from registry: ${params.reference}")
            
            def reference = params.reference
            def output = params.output ?: '.'
            
            return client.pull(
                reference: reference,
                output: output
            )
        }
    }
    
    /**
     * Copy artifacts between registries
     */
    void copy(Map params) {
        executeWithErrorHandling {
            logger.info("Copying from ${params.source} to ${params.target}")
            
            client.copy(
                source: params.source,
                target: params.target
            )
        }
    }
    
    /**
     * Attach files to existing artifact
     */
    Map attach(Map params) {
        return executeWithErrorHandling {
            logger.info("Attaching files to: ${params.reference}")
            
            return client.attach(
                reference: params.reference,
                files: params.files,
                annotations: params.annotations ?: [:],
                artifactType: params.artifactType
            )
        }
    }
    
    /**
     * Discover referrers of an artifact
     */
    List<Map> discover(Map params) {
        return executeWithErrorHandling {
            logger.info("Discovering referrers for: ${params.reference}")
            
            return client.discover(
                reference: params.reference,
                artifactType: params.artifactType
            )
        }
    }
    
    /**
     * Push Maven artifact to Nexus as OCI artifact
     */
    Map pushMavenArtifact(Map params) {
        return executeWithErrorHandling {
            logger.info("Pushing Maven artifact: ${params.artifactId}")
            
            def groupId = params.groupId
            def artifactId = params.artifactId
            def version = params.version
            def registry = params.registry ?: config.get('oras.registry')
            def repository = params.repository ?: config.get('oras.repository', 'maven')
            
            // Build reference
            def reference = "${registry}/${repository}/${groupId.replace('.', '/')}/${artifactId}:${version}"
            
            // Files to push
            def files = []
            def jarFile = params.jarFile ?: "target/${artifactId}-${version}.jar"
            def pomFile = params.pomFile ?: "pom.xml"
            
            if (script.fileExists(jarFile)) {
                files << "${jarFile}:application/java-archive"
            }
            
            if (script.fileExists(pomFile)) {
                files << "${pomFile}:application/xml"
            }
            
            // Add sources if present
            def sourcesFile = "target/${artifactId}-${version}-sources.jar"
            if (params.includeSources != false && script.fileExists(sourcesFile)) {
                files << "${sourcesFile}:application/java-archive"
            }
            
            // Add javadoc if present
            def javadocFile = "target/${artifactId}-${version}-javadoc.jar"
            if (params.includeJavadoc != false && script.fileExists(javadocFile)) {
                files << "${javadocFile}:application/java-archive"
            }
            
            // Annotations
            def annotations = [
                'org.opencontainers.image.created': new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'"),
                'org.opencontainers.image.authors': params.authors ?: 'Jenkins CI',
                'org.opencontainers.image.title': artifactId,
                'org.opencontainers.image.version': version,
                'maven.groupId': groupId,
                'maven.artifactId': artifactId,
                'maven.version': version
            ]
            
            if (params.annotations) {
                annotations += params.annotations
            }
            
            return push(
                reference: reference,
                files: files,
                annotations: annotations,
                artifactType: 'application/vnd.maven.artifact'
            )
        }
    }
    
    /**
     * Pull Maven artifact from Nexus OCI registry
     */
    Map pullMavenArtifact(Map params) {
        return executeWithErrorHandling {
            logger.info("Pulling Maven artifact: ${params.artifactId}")
            
            def groupId = params.groupId
            def artifactId = params.artifactId
            def version = params.version
            def registry = params.registry ?: config.get('oras.registry')
            def repository = params.repository ?: config.get('oras.repository', 'maven')
            def output = params.output ?: '.'
            
            def reference = "${registry}/${repository}/${groupId.replace('.', '/')}/${artifactId}:${version}"
            
            return pull(
                reference: reference,
                output: output
            )
        }
    }
    
    /**
     * Push generic files to Nexus
     */
    Map pushFiles(Map params) {
        return executeWithErrorHandling {
            logger.info("Pushing files: ${params.name}")
            
            def name = params.name
            def version = params.version ?: script.env.BUILD_NUMBER
            def registry = params.registry ?: config.get('oras.registry')
            def repository = params.repository ?: config.get('oras.repository', 'files')
            
            def reference = "${registry}/${repository}/${name}:${version}"
            
            return push(
                reference: reference,
                files: params.files,
                annotations: params.annotations ?: [:],
                artifactType: params.artifactType ?: 'application/vnd.oci.artifact'
            )
        }
    }
    
    /**
     * Tag artifact
     */
    void tag(Map params) {
        executeWithErrorHandling {
            logger.info("Tagging ${params.source} as ${params.target}")
            client.tag(params.source, params.target)
        }
    }
}
