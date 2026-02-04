package ado.clients

import ado.core.Config
import ado.utils.CommandExecutor

class OrasClient implements Serializable {
    private def script
    private Config config
    private CommandExecutor executor
    
    OrasClient(script, Config config) {
        this.script = script
        this.config = config
        this.executor = new CommandExecutor(script)
    }
    
    boolean isOrasAvailable() {
        try {
            executor.execute('oras version')
            return true
        } catch (Exception e) {
            return false
        }
    }
    
    void validateOrasInstallation() {
        if (!isOrasAvailable()) {
            throw new RuntimeException("ORAS is not installed or not available")
        }
    }
    
    Map push(Map params) {
        def reference = params.reference
        def files = params.files ?: []
        def annotations = params.annotations ?: [:]
        def config = params.config
        def mediaType = params.mediaType
        def artifactType = params.artifactType
        
        // Login to registry if credentials provided
        loginIfNeeded(reference)
        
        try {
            // Build command
            def cmd = ["oras push"]
            
            // Add artifact type
            if (artifactType) {
                cmd << "--artifact-type ${artifactType}"
            }
            
            // Add annotations
            annotations.each { key, value ->
                cmd << "--annotation \"${key}=${value}\""
            }
            
            // Add config if provided
            if (config) {
                cmd << "--config ${config}"
            }
            
            // Add reference
            cmd << reference
            
            // Add files with media types
            files.each { file ->
                if (file.contains(':')) {
                    // Format: file:mediaType
                    def parts = file.split(':', 2)
                    cmd << "${parts[0]}:${parts[1]}"
                } else {
                    // Auto-detect media type
                    cmd << file
                }
            }
            
            def command = cmd.join(' ')
            def output = executor.executeWithOutput(command)
            
            return [
                success: true,
                reference: reference,
                digest: extractDigest(output)
            ]
        } finally {
            logoutIfNeeded(reference)
        }
    }
    
    Map pull(Map params) {
        def reference = params.reference
        def output = params.output ?: '.'
        
        loginIfNeeded(reference)
        
        try {
            def command = "oras pull ${reference} --output ${output}"
            executor.execute(command)
            
            return [
                success: true,
                reference: reference,
                output: output
            ]
        } finally {
            logoutIfNeeded(reference)
        }
    }
    
    void copy(Map params) {
        def source = params.source
        def target = params.target
        
        loginIfNeeded(source)
        loginIfNeeded(target)
        
        try {
            executor.execute("oras copy ${source} ${target}")
        } finally {
            logoutIfNeeded(source)
            logoutIfNeeded(target)
        }
    }
    
    Map attach(Map params) {
        def reference = params.reference
        def files = params.files ?: []
        def annotations = params.annotations ?: [:]
        def artifactType = params.artifactType ?: 'application/vnd.oci.artifact'
        
        loginIfNeeded(reference)
        
        try {
            def cmd = ["oras attach"]
            
            cmd << "--artifact-type ${artifactType}"
            
            annotations.each { key, value ->
                cmd << "--annotation \"${key}=${value}\""
            }
            
            cmd << reference
            
            files.each { file ->
                cmd << file
            }
            
            def command = cmd.join(' ')
            def output = executor.executeWithOutput(command)
            
            return [
                success: true,
                reference: reference,
                digest: extractDigest(output)
            ]
        } finally {
            logoutIfNeeded(reference)
        }
    }
    
    List<Map> discover(Map params) {
        def reference = params.reference
        def artifactType = params.artifactType
        
        loginIfNeeded(reference)
        
        try {
            def cmd = "oras discover ${reference} --format json"
            
            if (artifactType) {
                cmd += " --artifact-type ${artifactType}"
            }
            
            def output = executor.executeWithOutput(cmd)
            
            // Parse JSON output
            def result = []
            try {
                result = script.readJSON(text: output)
            } catch (Exception e) {
                // Fallback
                result = []
            }
            
            return result
        } finally {
            logoutIfNeeded(reference)
        }
    }
    
    void tag(String source, String target) {
        loginIfNeeded(source)
        
        try {
            executor.execute("oras tag ${source} ${target}")
        } finally {
            logoutIfNeeded(source)
        }
    }
    
    private void loginIfNeeded(String reference) {
        def registry = extractRegistry(reference)
        def credentialsId = config.get('oras.credentialsId')
        
        if (credentialsId) {
            script.withCredentials([
                script.usernamePassword(
                    credentialsId: credentialsId,
                    usernameVariable: 'ORAS_USER',
                    passwordVariable: 'ORAS_PASS'
                )
            ]) {
                executor.execute("oras login -u ${script.env.ORAS_USER} --password-stdin ${registry} <<< '${script.env.ORAS_PASS}'")
            }
        }
    }
    
    private void logoutIfNeeded(String reference) {
        def registry = extractRegistry(reference)
        executor.execute("oras logout ${registry} || true", true)
    }
    
    private String extractRegistry(String reference) {
        // Extract registry from reference (e.g., nexus.company.com/repo/image:tag -> nexus.company.com)
        def parts = reference.split('/')
        return parts[0]
    }
    
    private String extractDigest(String output) {
        // Extract digest from output (e.g., "Pushed sha256:abc123...")
        def matcher = output =~ /sha256:[a-f0-9]{64}/
        return matcher ? matcher[0] : ''
    }
}
