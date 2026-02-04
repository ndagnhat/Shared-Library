package ado.core

class Logger implements Serializable {
    private def script
    private String context
    
    Logger(script, String context = '') {
        this.script = script
        this.context = context
    }
    
    void info(String message) {
        script.echo "[INFO] [${context}] ${message}"
    }
    
    void warn(String message) {
        script.echo "[WARN] [${context}] ${message}"
    }
    
    void error(String message) {
        script.echo "[ERROR] [${context}] ${message}"
    }
    
    void debug(String message) {
        if (script.env.DEBUG_MODE == 'true') {
            script.echo "[DEBUG] [${context}] ${message}"
        }
    }
}
