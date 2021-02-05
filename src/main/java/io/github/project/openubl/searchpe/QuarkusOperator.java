package io.github.project.openubl.searchpe;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.config.ConfigurationService;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

import javax.inject.Inject;

@QuarkusMain
public class QuarkusOperator implements QuarkusApplication {

    @Inject
    KubernetesClient client;

    @Inject
    Operator operator;

    @Inject
    ConfigurationService configuration;

    public static void main(String... args) {
        Quarkus.run(QuarkusOperator.class, args);
    }

    @Override
    public int run(String... args) throws Exception {
        operator.start();
        Quarkus.waitForExit();
        return 0;
    }
}
