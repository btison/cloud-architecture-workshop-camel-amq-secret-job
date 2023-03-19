package org.globex.retail.kubernetes;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class KubernetesRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesRunner.class);

    @Inject
    KubernetesClient client;

    public int run() {

        String amqNamespace = System.getenv("AMQ_NAMESPACE");
        String amqClientConnectionSecret = System.getenv().getOrDefault("AMQ_CLIENT_CONNECTION_SECRET", "client-amq");
        if (amqNamespace == null || amqNamespace.isBlank()) {
            LOGGER.error("Environment variable 'AMQ_NAMESPACE' for Amq broker namespace not set. Exiting...");
            return -1;
        }

        String namespace = System.getenv("NAMESPACE");
        if (namespace == null || namespace.isBlank()) {
            LOGGER.error("Environment variable 'NAMESPACE' for namespace not set. Exiting...");
            return -1;
        }

        Secret amqSecret = client.secrets().inNamespace(amqNamespace).withName(amqClientConnectionSecret).get();
        if (amqSecret == null) {
            LOGGER.error("Secret " + amqClientConnectionSecret + " not found in namespace " + amqNamespace);
            return -1;
        }

        Secret newSecret = new SecretBuilder().withNewMetadata().withName(amqClientConnectionSecret).endMetadata()
                .addToData(amqSecret.getData()).build();
        client.secrets().inNamespace(namespace).resource(newSecret).createOrReplace();

        LOGGER.info("Secret " + amqClientConnectionSecret + " created in namespace " + namespace + ". Exiting.");

        return 0;
    }
}
