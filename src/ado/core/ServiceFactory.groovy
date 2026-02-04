package ado.core

import ado.services.*

class ServiceFactory implements Serializable {
    private static final Map<String, Class> SERVICE_MAP = [
        'buildah': BuildahService,
        'oras': OrasService,
        'gitlab': GitlabService,
        'nexus': NexusService,
        'jira': JiraService,
        'argocd': ArgoCDService,
        'maven': MavenService,
        'nodejs': NodeJSService,
        'sonarqube': SonarQubeService,
        'scm': SCMService
    ]
    
    private def script
    private Config config
    private Map<String, BaseService> serviceCache = [:]
    
    ServiceFactory(script, Config config = null) {
        this.script = script
        this.config = config ?: new Config(script)
    }
    
    /**
     * Get or create service instance
     */
    def getService(String serviceName) {
        if (!serviceCache.containsKey(serviceName)) {
            def serviceClass = SERVICE_MAP[serviceName]
            if (!serviceClass) {
                throw new IllegalArgumentException("Unknown service: ${serviceName}")
            }
            
            def service = serviceClass.newInstance(script, config)
            service.initialize()
            serviceCache[serviceName] = service
        }
        
        return serviceCache[serviceName]
    }
    
    /**
     * Cleanup all services
     */
    void cleanupAll() {
        serviceCache.each { name, service ->
            service.cleanup()
        }
        serviceCache.clear()
    }
}
