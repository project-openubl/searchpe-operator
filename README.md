# searchpe-operator

## Start Minikube

```shell
minikube start
```

For more details about minikube visit the official [documentation](https://minikube.sigs.k8s.io/docs/start/).

## Operator container image

### Create application executable

You must choose between a native executable and a JVM executable

- Fast-jar executable:

```shell
./mvnw package -DskipTests -Dquarkus.package.type=fast-jar
```

- Native executable:

```shell
./mvnw package -Pnative -DskipTests -Dquarkus.native.container-build=true -Dquarkus.container-image.build=true -Dquarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-mandrel:20.3.0.0.Final-java11
```

### Create container image

We need to create the container image inside Minikube. Point your shell to minikube's docker-daemon:

```shell
eval $(minikube -p minikube docker-env)
```

- If you are using the Fast-jar mode:

```shell
docker build -f src/main/docker/Dockerfile.fast-jar -t webserver-operator .
```

- If you are using the Native mode:

```shell
docker build -f src/main/docker/Dockerfile.native -t webserver-operator .
```

## Deploy the CRD

```shell
kubectl create -f k8s/crd.yaml
```

## Deploy the Operator

```shell
kubectl create -f k8s/operator.yaml
```

## Instantiate the Operator

```shell
kubectl create -f k8s/searchpe.yaml -n webserver-operator
```

# Utils

Get pods:

```shell
kubectl get pods -n webserver-operator
```

Scale operator deployment:

```shell
kubectl scale deployments/webserver-operator --replicas=0 -n webserver-operator
```

Delete all:

```shell
kubectl delete searchpe hellows -n webserver-operator
kubectl delete namespace webserver-operator
kubectl delete customresourcedefinition searchpes.project.openubl
kubectl delete clusterrolebinding operator-admin
kubectl delete clusterroles webserver-operator
```
