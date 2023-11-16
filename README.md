# Meerkat Mango Service Registry

# How to Run

## Main
```shell
 ./mvnw spring-boot:run
```

## Backup
```shell
 ./mvnw spring-boot:run -Dspring-boot.run.profiles=backup
```

## Debug

```shell
./mvnw spring-boot:run  -Dspring-boot.run.profiles=backup -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=6060"
```
