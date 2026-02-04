package ado.models

class ContainerImage implements Serializable {
    String name
    String tag
    String dockerfile
    String context
    String registry
    
    String getFullName() {
        return "${name}:${tag}"
    }
    
    String getFullNameWithRegistry() {
        return registry ? "${registry}/${name}:${tag}" : getFullName()
    }
}
