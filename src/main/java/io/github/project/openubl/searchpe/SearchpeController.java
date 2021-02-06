package io.github.project.openubl.searchpe;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Controller
public class SearchpeController implements ResourceController<Searchpe> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final KubernetesClient kubernetesClient;

    public SearchpeController(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    @Override
    public UpdateControl<Searchpe> createOrUpdateResource(Searchpe searchpe, Context<Searchpe> context) {
        if (searchpe.getSpec().getHtml().contains("error")) {
            throw new ErrorSimulationException("Simulating error");
        }

        String ns = searchpe.getMetadata().getNamespace();

        Map<String, String> data = new HashMap<>();
        data.put("index.html", searchpe.getSpec().getHtml());

        ConfigMap htmlConfigMap =
                new ConfigMapBuilder()
                        .withMetadata(
                                new ObjectMetaBuilder()
                                        .withName(configMapName(searchpe))
                                        .withNamespace(ns)
                                        .build())
                        .withData(data)
                        .build();

        Deployment deployment = loadYaml(Deployment.class, "deployment.yaml");
        deployment.getMetadata().setName(deploymentName(searchpe));
        deployment.getMetadata().setNamespace(ns);
        deployment.getSpec().getSelector().getMatchLabels().put("app", deploymentName(searchpe));
        deployment
                .getSpec()
                .getTemplate()
                .getMetadata()
                .getLabels()
                .put("app", deploymentName(searchpe));
        deployment
                .getSpec()
                .getTemplate()
                .getSpec()
                .getVolumes()
                .get(0)
                .setConfigMap(
                        new ConfigMapVolumeSourceBuilder().withName(configMapName(searchpe)).build()
                );

        Service service = loadYaml(Service.class, "service.yaml");
        service.getMetadata().setName(serviceName(searchpe));
        service.getMetadata().setNamespace(ns);
        service.getSpec().setSelector(deployment.getSpec().getTemplate().getMetadata().getLabels());

        ConfigMap existingConfigMap =
                kubernetesClient
                        .configMaps()
                        .inNamespace(htmlConfigMap.getMetadata().getNamespace())
                        .withName(htmlConfigMap.getMetadata().getName())
                        .get();

        log.info("Creating or updating ConfigMap {} in {}", htmlConfigMap.getMetadata().getName(), ns);
        kubernetesClient.configMaps().inNamespace(ns).createOrReplace(htmlConfigMap);
        log.info("Creating or updating Deployment {} in {}", deployment.getMetadata().getName(), ns);
        kubernetesClient.apps().deployments().inNamespace(ns).createOrReplace(deployment);

        if (kubernetesClient.services().inNamespace(ns).withName(service.getMetadata().getName()).get() == null) {
            log.info("Creating Service {} in {}", service.getMetadata().getName(), ns);
            kubernetesClient.services().inNamespace(ns).createOrReplace(service);
        }

        if (existingConfigMap != null) {
            if (!StringUtils.equals(existingConfigMap.getData().get("index.html"), htmlConfigMap.getData().get("index.html"))) {
                log.info("Restarting pods because HTML has changed in {}", ns);
                kubernetesClient
                        .pods()
                        .inNamespace(ns)
                        .withLabel("app", deploymentName(searchpe))
                        .delete();
            }
        }

        SearchpeStatus status = new SearchpeStatus();
        status.setHtmlConfigMap(htmlConfigMap.getMetadata().getName());
        status.setAreWeGood("Yes!");
        searchpe.setStatus(status);
        //        throw new RuntimeException("Creating object failed, because it failed");
        return UpdateControl.updateCustomResource(searchpe);
    }

    @Override
    public DeleteControl deleteResource(Searchpe nginx, io.javaoperatorsdk.operator.api.Context<Searchpe> context) {
        log.info("Execution deleteResource for: {}", nginx.getMetadata().getName());

        log.info("Deleting ConfigMap {}", configMapName(nginx));
        Resource<ConfigMap> configMap =
                kubernetesClient
                        .configMaps()
                        .inNamespace(nginx.getMetadata().getNamespace())
                        .withName(configMapName(nginx));
        if (configMap.get() != null) {
            configMap.delete();
        }

        log.info("Deleting Deployment {}", deploymentName(nginx));
        RollableScalableResource<Deployment> deployment =
                kubernetesClient
                        .apps()
                        .deployments()
                        .inNamespace(nginx.getMetadata().getNamespace())
                        .withName(deploymentName(nginx));
        if (deployment.get() != null) {
            deployment.cascading(true).delete();
        }

        log.info("Deleting Service {}", serviceName(nginx));
        ServiceResource<Service> service =
                kubernetesClient
                        .services()
                        .inNamespace(nginx.getMetadata().getNamespace())
                        .withName(serviceName(nginx));
        if (service.get() != null) {
            service.delete();
        }
        return DeleteControl.DEFAULT_DELETE;
    }

    private static String configMapName(Searchpe nginx) {
        return nginx.getMetadata().getName() + "-html";
    }

    private static String deploymentName(Searchpe nginx) {
        return nginx.getMetadata().getName();
    }

    private static String serviceName(Searchpe nginx) {
        return nginx.getMetadata().getName();
    }

    private <T> T loadYaml(Class<T> clazz, String yaml) {
        try (InputStream is = getClass().getResourceAsStream(yaml)) {
            return Serialization.unmarshal(is, clazz);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot find yaml on classpath: " + yaml);
        }
    }

}
