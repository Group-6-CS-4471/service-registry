# Meerkat Mango Service Registry

## How it works 

`ServiceRegistryResource` is the main class responsible for accepting requests to register a service provider. 
`HealthCheckExecutor`'s main job is to call the health endpoints of the registered providers to check livelihood. 
- if they aren't healthy, they're remove from the list of providers
  - if no providers are healthy, then we say the service is unhealthy.
- if they are healthy, we return true

## How Backup Registry Works
The backup registry is on a different profile. `BackupRegistryExecutor` runs a task to periodically call the main registry 
and retrieve an updated set of services and whether they are healthy or not. 

## How to Run Locally

### Main
```shell
 ./mvnw spring-boot:run
```

### Backup
```shell
 ./mvnw spring-boot:run -Dspring-boot.run.profiles=backup
```

### Debug

```shell
./mvnw spring-boot:run  -Dspring-boot.run.profiles=backup -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=6060"
```

## Deployment 

### Packaging the JAR
```shell
mvn clean install
```

This will create a jar file called service-registry-0.0.1-SNAPSHOT.jar in the target directory. 

For deployment / running purposes this is all you will need. 

### Running the JAR
Assumption: JAR is in current directory

The following commands will run the program on port 50001 and 50000 respectively. 

For backup:
```shell
java -jar service-registry-0.0.1-SNAPSHOT.jar --spring.profiles.active=backup
```

For Main:
```shell
java -jar service-registry-0.0.1-SNAPSHOT.jar
```