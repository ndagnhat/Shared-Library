package ado.utils

class CommandExecutor implements Serializable {
    private def script
    
    CommandExecutor(script) {
        this.script = script
    }
    
    String execute(String command, boolean returnStatus = false) {
        if (returnStatus) {
            return script.sh(script: command, returnStatus: true)
        } else {
            return script.sh(script: command, returnStdout: true).trim()
        }
    }
    
    String executeWithOutput(String command) {
        return script.sh(script: command, returnStdout: true).trim()
    }
    
    int executeWithStatusCode(String command) {
        return script.sh(script: command, returnStatus: true)
    }
}
