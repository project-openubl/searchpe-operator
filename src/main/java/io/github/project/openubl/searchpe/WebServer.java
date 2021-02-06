package io.github.project.openubl.searchpe;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("project.openubl")
@Version("v1")
public class WebServer extends CustomResource<ServiceSpec, WebServerStatus> implements Namespaced {
}
