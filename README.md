# Spring Leader Election with Kubernetes

Aplicación Spring Boot que implementa la elección de líder en Kubernetes.

## Requisitos

- Java 17
- Maven
- Docker
- Minikube
- Kubernetes CLI (kubectl)

## Compilación

```sh
mvn clean install
```

## Imagen Docker en minikube

```sh
eval $(minikube docker-env)
docker build -t spring-leader-election:latest .
```

## Comandos minikube

Iniciar minikube 

```sh
minikube start
```

Parar minikube

```sh
minikube stop
```

Borrar entorno

```sh
minikube delete
```

## Desplegar componentes en Kubernetes

Define los roles necesarios para la aplicación

```sh
kubectl apply -f kubernetes/lease-role.yaml
```

Crea una cuenta de servicio para la aplicación

```sh
kubectl apply -f kubernetes/service-account.yaml
```

Asocia la cuenta de servicio con los roles definidos

```sh
kubectl apply -f kubernetes/lease-rolebinding.yaml
```

Define el lease que se utilizará para la elección de líder

```sh
kubectl apply -f kubernetes/lease.yaml
```

Despliega la aplicación en Kubernetes

```sh
kubectl apply -f kubernetes/deployment.yaml
```

## Comandos Kubernetes

Verificar el despliegue de pods del `deployment`

```sh
kubectl get pods
```

Verificar los logs del deployment

```sh
kubectl logs -l app=spring-leader-election
```

```sh
kubectl logs -l app=spring-leader-election --tail=100 --follow
```

Matar un pod

```sh
kubectl delete pod {}
```

Reiniciar deployment

```sh
kubectl rollout restart deployment spring-leader-election
```

Escalar replicas (modificar 0 con número deseado)

```sh
kubectl scale deployment spring-leader-election --replicas 0
```

## Demo

Despliega el deployment de kubernetes. Cuando revises los logs, podras ver como entre los diferentes pods se coordina el líder. Ejemplo:

```
{"timestamp":"2025-02-05 10:05:54.309", "level":"INFO", "message":"[7160e425-14ba-4dd5-a1ae-a47c3526fe40] lease held by: 73f1e22e-0462-43b8-a5e8-33d7a0556ec1"}
{"timestamp":"2025-02-05 10:05:54.711", "level":"INFO", "message":"Tomcat started on port 8080 (http) with context path '/'"}
{"timestamp":"2025-02-05 10:05:55.259", "level":"INFO", "message":"Started DemoApplication in 119.0 seconds (process running for 137.421)"}
{"timestamp":"2025-02-05 10:05:55.536", "level":"INFO", "message":"[73f1e22e-0462-43b8-a5e8-33d7a0556ec1] lease held by: 73f1e22e-0462-43b8-a5e8-33d7a0556ec1"}
{"timestamp":"2025-02-05 10:05:55.970", "level":"INFO", "message":"[069d2f63-80a1-4425-b449-1ad451cd22e0] lease held by: 73f1e22e-0462-43b8-a5e8-33d7a0556ec1"}
{"timestamp":"2025-02-05 10:05:58.108", "level":"INFO", "message":"[06374158-eaff-4f37-92b7-e9b8264cdf48] lease held by: 73f1e22e-0462-43b8-a5e8-33d7a0556ec1"}
{"timestamp":"2025-02-05 10:05:59.405", "level":"INFO", "message":"[7160e425-14ba-4dd5-a1ae-a47c3526fe40] lease held by: 73f1e22e-0462-43b8-a5e8-33d7a0556ec1"}
{"timestamp":"2025-02-05 10:06:00.005", "level":"INFO", "message":"[0475aedf-47f3-445f-ba37-a8ad4a356f43] lease held by: 73f1e22e-0462-43b8-a5e8-33d7a0556ec1"}
{"timestamp":"2025-02-05 10:06:00.576", "level":"INFO", "message":"[73f1e22e-0462-43b8-a5e8-33d7a0556ec1] lease held by: 73f1e22e-0462-43b8-a5e8-33d7a0556ec1"}
{"timestamp":"2025-02-05 10:06:01.041", "level":"INFO", "message":"[069d2f63-80a1-4425-b449-1ad451cd22e0] lease held by: 73f1e22e-0462-43b8-a5e8-33d7a0556ec1"}
{"timestamp":"2025-02-05 10:06:03.140", "level":"INFO", "message":"[06374158-eaff-4f37-92b7-e9b8264cdf48] lease held by: 73f1e22e-0462-43b8-a5e8-33d7a0556ec1"}
{"timestamp":"2025-02-05 10:06:04.452", "level":"INFO", "message":"[7160e425-14ba-4dd5-a1ae-a47c3526fe40] lease held by: 73f1e22e-0462-43b8-a5e8-33d7a0556ec1"}
{"timestamp":"2025-02-05 10:06:05.079", "level":"INFO", "message":"[0475aedf-47f3-445f-ba37-a8ad4a356f43] lease held by: 73f1e22e-0462-43b8-a5e8-33d7a0556ec1"}
{"timestamp":"2025-02-05 10:06:05.615", "level":"INFO", "message":"[73f1e22e-0462-43b8-a5e8-33d7a0556ec1] lease held by: 73f1e22e-0462-43b8-a5e8-33d7a0556ec1"}
{"timestamp":"2025-02-05 10:06:06.076", "level":"INFO", "message":"[069d2f63-80a1-4425-b449-1ad451cd22e0] lease held by: 73f1e22e-0462-43b8-a5e8-33d7a0556ec1"}
```

Se puede ver tambien como el líder renueva con preferencia:

```
{"timestamp":"2025-02-05 10:08:06.745", "level":"INFO", "message":"[73f1e22e-0462-43b8-a5e8-33d7a0556ec1] lease: LeaseSpec(acquireTime=null, holderIdentity=73f1e22e-0462-43b8-a5e8-33d7a0556ec1, leaseDurationSeconds=60, leaseTransitions=null, preferredHolder=null, renewTime=2025-02-05T10:07:21.368014Z, strategy=null, additionalProperties={})"}
{"timestamp":"2025-02-05 10:08:06.852", "level":"INFO", "message":"[73f1e22e-0462-43b8-a5e8-33d7a0556ec1] lease acquired"}
{"timestamp":"2025-02-05 10:08:07.159", "level":"INFO", "message":"[069d2f63-80a1-4425-b449-1ad451cd22e0] lease held by: 73f1e22e-0462-43b8-a5e8-33d7a0556ec1"}
{"timestamp":"2025-02-05 10:08:09.598", "level":"INFO", "message":"[06374158-eaff-4f37-92b7-e9b8264cdf48] lease held by: 73f1e22e-0462-43b8-a5e8-33d7a0556ec1"}
```

Si el pod del líder se apaga, se asigna uno nuevo

```
{"timestamp":"2025-02-05 10:09:32.037", "level":"INFO", "message":"[0475aedf-47f3-445f-ba37-a8ad4a356f43] lease: LeaseSpec(acquireTime=null, holderIdentity=null, leaseDurationSeconds=60, leaseTransitions=null, preferredHolder=null, renewTime=2025-02-05T10:09:31.047951Z, strategy=null, additionalProperties={})"}
{"timestamp":"2025-02-05 10:09:32.615", "level":"INFO", "message":"[0475aedf-47f3-445f-ba37-a8ad4a356f43] lease acquired"}
{"timestamp":"2025-02-05 10:09:34.772", "level":"INFO", "message":"[06374158-eaff-4f37-92b7-e9b8264cdf48] lease held by: 0475aedf-47f3-445f-ba37-a8ad4a356f43"}
{"timestamp":"2025-02-05 10:09:37.641", "level":"INFO", "message":"[0475aedf-47f3-445f-ba37-a8ad4a356f43] lease held by: 0475aedf-47f3-445f-ba37-a8ad4a356f43"}
{"timestamp":"2025-02-05 10:09:39.752", "level":"INFO", "message":"[069d2f63-80a1-4425-b449-1ad451cd22e0] lease held by: 0475aedf-47f3-445f-ba37-a8ad4a356f43"}
```
