import ado.core.ServiceFactory
import ado.core.Config

def call(Map params = [:]) {
    def config = new Config(this)
    def factory = new ServiceFactory(this, config)
    
    def buildah = factory.getService('buildah')
    
    try {
        def image = buildah.build(params)
        
        // Auto-push if requested
        if (params.push) {
            buildah.push(image, params)
        }
        
        // Auto-scan if requested
        if (params.scan) {
            buildah.scan(image, params)
        }
        
        return image
    } finally {
        factory.cleanupAll()
    }
}
