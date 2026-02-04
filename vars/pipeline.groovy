import ado.core.ServiceFactory
import ado.core.Config

def call(Closure body) {
    def config = new Config(this)
    def factory = new ServiceFactory(this, config)
    
    // Make services available to pipeline
    def services = [
        buildah: factory.getService('buildah'),
        oras: factory.getService('oras'),
        gitlab: factory.getService('gitlab'),
        nexus: factory.getService('nexus'),
        jira: factory.getService('jira'),
        argocd: factory.getService('argocd'),
        maven: factory.getService('maven'),
        nodejs: factory.getService('nodejs'),
        sonarqube: factory.getService('sonarqube'),
        scm: factory.getService('scm')
    ]
    
    try {
        body.call(services)
    } finally {
        factory.cleanupAll()
    }
}
