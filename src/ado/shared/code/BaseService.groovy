// BaseService.groovy

// Base class for services

abstract class BaseService {
    // Common properties and methods for all services
    String serviceName

    BaseService(String serviceName) {
        this.serviceName = serviceName
    }

    abstract void execute() // Must be implemented by derived classes
}