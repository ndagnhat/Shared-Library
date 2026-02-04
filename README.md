# Jenkins Shared Library - Buildah + ORAS Edition

Jenkins Shared Library with Buildah (Docker replacement) and ORAS integration for storing artifacts in Nexus OCI registry.

## 🚀 Features

### Buildah Service (Docker Replacement)
- ✅ **Daemonless** container image building
- ✅ **Rootless** builds for better security
- ✅ **OCI-compliant** images
- ✅ Multi-architecture builds
- ✅ Build, push, pull, scan images
- ✅ Compatible with existing Docker registries

### ORAS Service
- ✅ Store **any files** to OCI registry (Nexus)
- ✅ Maven artifacts as OCI artifacts
- ✅ Generic file storage
- ✅ Attach metadata and annotations
- ✅ Discover and manage artifacts

### Additional Services
- GitLab integration
- Nexus repository management
- Jira integration
- ArgoCD deployment
- Maven builds
- Node.js builds
- SonarQube analysis
- SCM operations

## 📁 Structure

```
jenkins-shared-library/
├── src/
│   └── org/company/
│       ├── core/                 # Core framework
│       │   ├── BaseService.groovy
│       │   ├── ServiceFactory.groovy
│       │   ├── Config.groovy
│       │   └── Logger.groovy
│       ├── services/             # Business logic
│       │   ├── BuildahService.groovy
│       │   ├── OrasService.groovy
│       │   └── ...
│       ├── clients/              # API/Command clients
│       │   ├── BuildahClient.groovy
│       │   ├── OrasClient.groovy
│       │   └── ...
│       ├── utils/                # Utilities
│       │   └── CommandExecutor.groovy
│       └── models/               # Data models
│           └── ContainerImage.groovy
├── vars/                         # Pipeline DSL
│   ├── pipeline.groovy
│   ├── buildahBuild.groovy
│   ├── orasPush.groovy
│   └── orasPull.groovy
└── resources/
    └── config/
        └── default-config.yaml
```

## 🔧 Installation

### 1. Install Buildah

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install -y buildah

# RHEL/CentOS
sudo yum install -y buildah

# Verify installation
buildah --version
```

### 2. Install ORAS

```bash
# Download and install ORAS
VERSION="1.1.0"
curl -LO "https://github.com/oras-project/oras/releases/download/v${VERSION}/oras_${VERSION}_linux_amd64.tar.gz"
mkdir -p oras-install/
tar -zxf oras_${VERSION}_*.tar.gz -C oras-install/
sudo mv oras-install/oras /usr/local/bin/
rm -rf oras_${VERSION}_*.tar.gz oras-install/

# Verify installation
oras version
```

### 3. Configure Jenkins

Add this library to Jenkins:
1. Go to **Manage Jenkins** → **Configure System**
2. Find **Global Pipeline Libraries**
3. Add library with name `company-shared-library`
4. Set retrieval method (Git, etc.)

## 📖 Usage Examples

### Example 1: Build with Buildah

```groovy
@Library('company-shared-library') _

pipeline {
    agent any
    
    stages {
        stage('Build Image') {
            steps {
                script {
                    buildahBuild(
                        imageName: 'myapp',
                        imageTag: env.BUILD_NUMBER,
                        dockerfile: 'Containerfile',
                        buildArgs: [
                            VERSION: env.BUILD_NUMBER
                        ],
                        push: true,
                        registry: 'nexus.company.com',
                        credentialsId: 'nexus-credentials'
                    )
                }
            }
        }
    }
}
```

### Example 2: Push Files with ORAS

```groovy
@Library('company-shared-library') _

pipeline {
    agent any
    
    stages {
        stage('Push Artifacts') {
            steps {
                script {
                    orasPush(
                        reference: 'nexus.company.com/artifacts/myapp:1.0.0',
                        files: [
                            'target/myapp.jar:application/java-archive',
                            'pom.xml:application/xml',
                            'README.md:text/markdown'
                        ],
                        annotations: [
                            'org.opencontainers.image.created': new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'"),
                            'org.opencontainers.image.version': '1.0.0',
                            'build.number': env.BUILD_NUMBER
                        ]
                    )
                }
            }
        }
    }
}
```

### Example 3: Push Maven Artifact to Nexus OCI

```groovy
@Library('company-shared-library') _

pipeline {
    agent any
    
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean package'
            }
        }
        
        stage('Push to Nexus OCI') {
            steps {
                script {
                    pipeline { services ->
                        services.oras.pushMavenArtifact(
                            groupId: 'com.company',
                            artifactId: 'myapp',
                            version: '1.0.0',
                            registry: 'nexus.company.com',
                            repository: 'maven-oci',
                            includeSources: true,
                            includeJavadoc: true
                        )
                    }
                }
            }
        }
    }
}
```

### Example 4: Full Pipeline with Buildah + ORAS

```groovy
@Library('company-shared-library') _

pipeline {
    agent any
    
    environment {
        REGISTRY = 'nexus.company.com'
        IMAGE_NAME = 'myapp'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build Application') {
            steps {
                sh 'mvn clean package'
            }
        }
        
        stage('Push Maven Artifacts (ORAS)') {
            steps {
                script {
                    pipeline { services ->
                        services.oras.pushMavenArtifact(
                            groupId: 'com.company',
                            artifactId: env.IMAGE_NAME,
                            version: env.IMAGE_TAG,
                            includeSources: true
                        )
                    }
                }
            }
        }
        
        stage('Build Container Image (Buildah)') {
            steps {
                script {
                    pipeline { services ->
                        def image = services.buildah.build(
                            imageName: env.IMAGE_NAME,
                            imageTag: env.IMAGE_TAG,
                            dockerfile: 'Containerfile',
                            buildArgs: [
                                JAR_FILE: "target/${env.IMAGE_NAME}-${env.IMAGE_TAG}.jar"
                            ]
                        )
                        
                        // Scan for vulnerabilities
                        services.buildah.scan(image, 
                            tool: 'trivy',
                            severity: 'HIGH,CRITICAL'
                        )
                        
                        // Push to registry
                        services.buildah.push(image,
                            registry: env.REGISTRY,
                            credentialsId: 'nexus-credentials'
                        )
                    }
                }
            }
        }
        
        stage('Push Additional Files (ORAS)') {
            steps {
                script {
                    orasPush(
                        reference: "${env.REGISTRY}/docs/${env.IMAGE_NAME}:${env.IMAGE_TAG}",
                        files: [
                            'README.md:text/markdown',
                            'CHANGELOG.md:text/markdown',
                            'docs/api.html:text/html'
                        ],
                        annotations: [
                            'build.number': env.BUILD_NUMBER,
                            'git.commit': env.GIT_COMMIT
                        ]
                    )
                }
            }
        }
    }
}
```

### Example 5: Multi-Architecture Build

```groovy
@Library('company-shared-library') _

pipeline {
    agent any
    
    stages {
        stage('Build Multi-Arch') {
            steps {
                script {
                    pipeline { services ->
                        services.buildah.buildMultiArch(
                            imageName: 'myapp',
                            imageTag: '1.0.0',
                            platforms: ['linux/amd64', 'linux/arm64', 'linux/arm/v7'],
                            dockerfile: 'Containerfile',
                            push: true,
                            registry: 'nexus.company.com',
                            credentialsId: 'nexus-credentials'
                        )
                    }
                }
            }
        }
    }
}
```

## 🔒 Security

### Buildah Security Advantages
- No daemon required (eliminates daemon attack surface)
- Rootless builds supported
- Runs without elevated privileges
- Better isolation with different isolation modes

### ORAS Security
- Standard OCI registry authentication
- Supports signed artifacts
- Metadata and provenance tracking

## 🔑 Credentials Setup

### Nexus/Registry Credentials
1. Go to Jenkins → Credentials
2. Add Username/Password credential
3. ID: `nexus-credentials` or custom ID
4. Use in pipelines via `credentialsId` parameter

### Example Credentials Config
```groovy
// In Jenkinsfile
environment {
    REGISTRY_CREDS = credentials('nexus-credentials')
}
```

## ⚙️ Configuration

Default configuration in `Config.groovy`:

```groovy
buildah: [
    registry: 'docker.io',
    credentialsId: 'container-registry-credentials',
    format: 'oci',
    isolation: 'chroot'
],
oras: [
    registry: 'nexus.company.com',
    credentialsId: 'nexus-credentials',
    repository: 'oci-artifacts'
]
```

Override in pipeline:
```groovy
pipeline { services ->
    services.config.set('buildah.registry', 'my-registry.com')
    services.config.set('oras.repository', 'my-repo')
    // ...
}
```

## 🆚 Buildah vs Docker

| Feature | Buildah | Docker |
|---------|---------|--------|
| Daemon | ❌ No daemon | ✅ Requires daemon |
| Rootless | ✅ Full support | ⚠️ Limited |
| OCI Standard | ✅ Native | ✅ Supported |
| Build Command | `buildah bud` | `docker build` |
| Push Command | `buildah push` | `docker push` |
| Image Format | OCI/Docker | Docker |

## 📝 Migration from Docker

Replace Docker commands:
```groovy
// Before (Docker)
docker.build("myapp:1.0")
docker.push("myapp:1.0")

// After (Buildah)
buildahBuild(
    imageName: 'myapp',
    imageTag: '1.0',
    push: true
)
```

## 🐛 Troubleshooting

### Buildah not found
```bash
# Install buildah
sudo apt-get install buildah

# Verify
buildah --version
```

### ORAS not found
```bash
# Install ORAS
curl -LO https://github.com/oras-project/oras/releases/download/v1.1.0/oras_1.1.0_linux_amd64.tar.gz
sudo tar -zxf oras_1.1.0_linux_amd64.tar.gz -C /usr/local/bin oras
rm oras_1.1.0_linux_amd64.tar.gz
```

### Permission denied
```bash
# Add user to necessary groups
sudo usermod -aG containers jenkins

# For rootless buildah
buildah unshare
```

## 📚 Additional Resources

- [Buildah Documentation](https://buildah.io/)
- [ORAS Documentation](https://oras.land/)
- [OCI Spec](https://github.com/opencontainers/image-spec)
- [Nexus OCI Registry](https://help.sonatype.com/repomanager3/nexus-repository-administration/formats/docker-registry)

## 🤝 Contributing

Contributions welcome! Please follow the existing code structure and patterns.

## 📄 License

Internal use only - Company Confidential
