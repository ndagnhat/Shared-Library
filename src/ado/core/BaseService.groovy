package ado.core

abstract class BaseService implements Serializable {
    protected def script
    protected Logger logger
    protected Config config
    
    BaseService(script, Config config = null) {
        this.script = script
        this.config = config ?: new Config(script)
        this.logger = new Logger(script, this.class.simpleName)
    }
    
    /**
     * Initialize service with configuration
     */
    abstract void initialize()
    
    /**
     * Validate service configuration
     */
    abstract boolean validate()
    
    /**
     * Cleanup resources
     */
    void cleanup() {
        logger.info("Cleaning up ${this.class.simpleName}")
    }
    
    /**
     * Execute with error handling
     */
    protected def executeWithErrorHandling(Closure action) {
        try {
            return action()
        } catch (Exception e) {
            logger.error("Error in ${this.class.simpleName}: ${e.message}")
            throw e
        }
    }
}
