package ado.core

class Config implements Serializable {
    private def script
    private Map configData = [:]
    
    Config(script) {
        this.script = script
        loadDefaultConfig()
    }
    
    private void loadDefaultConfig() {
        configData = [
            buildah: [
                registry: script.env.BUILDAH_REGISTRY ?: script.env.CONTAINER_REGISTRY ?: 'docker.io',
                credentialsId: 'container-registry-credentials',
                format: 'oci', // oci or docker
                isolation: 'chroot' // chroot, rootless, or oci
            ],
            oras: [
                registry: script.env.NEXUS_URL?.replaceAll('https?://', '') ?: 'nexus.company.com',
                credentialsId: 'nexus-credentials',
                repository: 'oci-artifacts'
            ],
            gitlab: [
                url: script.env.GITLAB_URL ?: 'https://gitlab.com',
                credentialsId: 'gitlab-token'
            ],
            nexus: [
                url: script.env.NEXUS_URL ?: 'https://nexus.company.com',
                credentialsId: 'nexus-credentials'
            ],
            jira: [
                url: script.env.JIRA_URL ?: 'https://jira.company.com',
                credentialsId: 'jira-token'
            ],
            argocd: [
                url: script.env.ARGOCD_URL ?: 'https://argocd.company.com',
                credentialsId: 'argocd-token'
            ],
            sonarqube: [
                url: script.env.SONARQUBE_URL ?: 'https://sonarqube.company.com',
                credentialsId: 'sonarqube-token'
            ]
        ]
    }
    
    def get(String key, def defaultValue = null) {
        def keys = key.split('\\.')
        def value = configData
        
        keys.each { k ->
            value = value?."${k}"
        }
        
        return value ?: defaultValue
    }
    
    void set(String key, def value) {
        def keys = key.split('\\.')
        def current = configData
        
        keys[0..-2].each { k ->
            if (!current[k]) {
                current[k] = [:]
            }
            current = current[k]
        }
        
        current[keys[-1]] = value
    }
    
    Map getAll() {
        return configData
    }
}
