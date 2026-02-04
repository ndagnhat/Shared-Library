import ado.core.ServiceFactory
import ado.core.Config

def call(Map params = [:]) {
    def config = new Config(this)
    def factory = new ServiceFactory(this, config)
    
    def oras = factory.getService('oras')
    
    try {
        return oras.pull(params)
    } finally {
        factory.cleanupAll()
    }
}
