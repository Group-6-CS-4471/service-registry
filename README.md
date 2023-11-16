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

## How to Run

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
