package ado.clients

import ado.core.Config
import ado.models.ContainerImage
import ado.utils.CommandExecutor

class BuildahClient implements Serializable {
    private def script
    private Config config
    private CommandExecutor executor
    
    BuildahClient(script, Config config) {
        this.script = script
        this.config = config
        this.executor = new CommandExecutor(script)
    }
    
    boolean isBuildahAvailable() {
        try {
            executor.execute('buildah --version')
            return true
        } catch (Exception e) {
            return false
        }
    }
    
    void validateBuildahInstallation() {
        if (!isBuildahAvailable()) {
            throw new RuntimeException("Buildah is not installed or not available")
        }
    }
    
    void build(ContainerImage image, Map params) {
        def buildArgs = params.buildArgs ?: [:]
        def noCache = params.noCache ?: false
        def target = params.target ?: ''
        def format = params.format ?: config.get('buildah.format', 'oci')
        def isolation = params.isolation ?: config.get('buildah.isolation', 'chroot')
        def layers = params.layers != false
        
        def buildArgsString = buildArgs.collect { key, value -> 
            "--build-arg ${key}=${value}" 
        }.join(' ')
        
        def noCacheFlag = noCache ? '--no-cache' : ''
        def targetFlag = target ? "--target ${target}" : ''
        def layersFlag = layers ? '--layers' : ''
        
        def command = """
            buildah bud ${noCacheFlag} ${targetFlag} ${layersFlag} \\
                --format=${format} \\
                --isolation=${isolation} \\
                -t ${image.fullName} \\
                -t ${image.name}:latest \\
                -f ${image.dockerfile} \\
                ${buildArgsString} \\
                ${image.context}
        """
        
        executor.execute(command)
    }
    
    void push(ContainerImage image, String registry, String credentialsId) {
        def fullImageName = registry ? "${registry}/${image.name}" : image.name
        
        // Login to registry
        script.withCredentials([
            script.usernamePassword(
                credentialsId: credentialsId,
                usernameVariable: 'REGISTRY_USER',
                passwordVariable: 'REGISTRY_PASS'
            )
        ]) {
            executor.execute("buildah login -u ${script.env.REGISTRY_USER} --password-stdin ${registry} <<< '${script.env.REGISTRY_PASS}'")
        }
        
        try {
            // Tag with registry prefix if needed
            if (registry) {
                executor.execute("buildah tag ${image.fullName} ${fullImageName}:${image.tag}")
                executor.execute("buildah tag ${image.name}:latest ${fullImageName}:latest")
            }
            
            // Push images
            executor.execute("buildah push ${fullImageName}:${image.tag}")
            executor.execute("buildah push ${fullImageName}:latest")
        } finally {
            // Logout
            executor.execute("buildah logout ${registry}")
        }
    }
    
    Map scan(ContainerImage image, String tool, String severity) {
        def reportFile = "${tool}-report.json"
        
        def exitCode = 0
        
        switch(tool) {
            case 'trivy':
                exitCode = executor.execute(
                    """
                    trivy image \\
                        --severity ${severity} \\
                        --format json \\
                        --output ${reportFile} \\
                        ${image.fullName}
                    """,
                    true
                )
                break
                
            case 'grype':
                exitCode = executor.execute(
                    """
                    grype ${image.fullName} \\
                        --fail-on ${severity.toLowerCase()} \\
                        -o json \\
                        --file ${reportFile}
                    """,
                    true
                )
                break
                
            default:
                throw new IllegalArgumentException("Unsupported scan tool: ${tool}")
        }
        
        return [
            success: exitCode == 0,
            reportFile: reportFile,
            exitCode: exitCode
        ]
    }
    
    void buildMultiArch(ContainerImage image, List<String> platforms, Map params) {
        def manifestName = image.fullName
        
        // Create manifest
        executor.execute("buildah manifest create ${manifestName}")
        
        // Build for each platform
        platforms.each { platform ->
            def platformParts = platform.split('/')
            def os = platformParts[0]
            def arch = platformParts[1]
            
            def buildArgs = params.buildArgs ?: [:]
            def buildArgsString = buildArgs.collect { key, value -> 
                "--build-arg ${key}=${value}" 
            }.join(' ')
            
            def command = """
                buildah bud \\
                    --os=${os} \\
                    --arch=${arch} \\
                    --manifest ${manifestName} \\
                    -f ${params.dockerfile ?: 'Containerfile'} \\
                    ${buildArgsString} \\
                    ${params.context ?: '.'}
            """
            
            executor.execute(command)
        }
        
        // Push manifest if requested
        if (params.push) {
            def registry = params.registry ?: config.get('buildah.registry')
            def credentialsId = params.credentialsId ?: config.get('buildah.credentialsId')
            
            script.withCredentials([
                script.usernamePassword(
                    credentialsId: credentialsId,
                    usernameVariable: 'REGISTRY_USER',
                    passwordVariable: 'REGISTRY_PASS'
                )
            ]) {
                executor.execute("buildah login -u ${script.env.REGISTRY_USER} --password-stdin ${registry} <<< '${script.env.REGISTRY_PASS}'")
                executor.execute("buildah manifest push --all ${manifestName} docker://${manifestName}")
                executor.execute("buildah logout ${registry}")
            }
        }
    }
    
    String runContainer(ContainerImage image, Map params) {
        def containerName = params.containerName ?: "test-${script.env.BUILD_NUMBER}"
        def ports = params.ports ?: []
        def env = params.env ?: [:]
        def volumes = params.volumes ?: [:]
        def command = params.command ?: ''
        
        def portMappings = ports.collect { "-p ${it}" }.join(' ')
        def envVars = env.collect { k, v -> "-e ${k}=${v}" }.join(' ')
        def volumeMappings = volumes.collect { k, v -> "-v ${k}:${v}" }.join(' ')
        
        // Use podman to run (buildah doesn't run containers)
        executor.execute("""
            podman run -d \\
                --name ${containerName} \\
                ${portMappings} \\
                ${envVars} \\
                ${volumeMappings} \\
                ${image.fullName} \\
                ${command}
        """)
        
        return containerName
    }
    
    void stopContainer(String containerName) {
        executor.execute("podman stop ${containerName} || true", true)
        executor.execute("podman rm ${containerName} || true", true)
    }
    
    void tag(ContainerImage image, String newTag) {
        executor.execute("buildah tag ${image.fullName} ${image.name}:${newTag}")
    }
    
    void pull(String imageName, String registry, String credentialsId) {
        def fullImageName = registry ? "${registry}/${imageName}" : imageName
        
        script.withCredentials([
            script.usernamePassword(
                credentialsId: credentialsId,
                usernameVariable: 'REGISTRY_USER',
                passwordVariable: 'REGISTRY_PASS'
            )
        ]) {
            executor.execute("buildah login -u ${script.env.REGISTRY_USER} --password-stdin ${registry} <<< '${script.env.REGISTRY_PASS}'")
            executor.execute("buildah pull ${fullImageName}")
            executor.execute("buildah logout ${registry}")
        }
    }
    
    List<Map> listImages(Map params) {
        def filter = params.filter ?: ''
        def filterFlag = filter ? "--filter ${filter}" : ''
        
        def output = executor.executeWithOutput("buildah images ${filterFlag} --format json")
        
        // Parse JSON output
        def images = []
        try {
            images = script.readJSON(text: output)
        } catch (Exception e) {
            // Fallback to simple parsing
            images = []
        }
        
        return images
    }
    
    void removeImage(String imageName, Map params) {
        def force = params.force ?: false
        def forceFlag = force ? '-f' : ''
        
        executor.execute("buildah rmi ${forceFlag} ${imageName}", true)
    }
    
    void cleanup() {
        // Clean up containers and images
        executor.execute('buildah rm --all || true', true)
        executor.execute('buildah rmi --prune || true', true)
    }
}
