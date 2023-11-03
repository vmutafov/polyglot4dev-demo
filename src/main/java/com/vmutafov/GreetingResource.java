package com.vmutafov;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Path("/hello")
public class GreetingResource {

    @Inject
    private ObjectMapper objectMapper;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String hello() throws IOException {
        return executeJs("handler.js", "onGet");
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public String onPost(Data data) throws IOException {
        return executeJs("handler.js", "onPost", data);
    }

    private String executeJs(
            String jsName,
            String jsFuncName,
            Object... args
    ) throws IOException {
        try (var context = Context.newBuilder().allowAllAccess(true).build()) {
            var source = resourceAsString("/META-INF/resources/js/" + jsName);
            context.eval("js", source);

            Value res = context
                    .getBindings("js")
                    .getMember(jsFuncName)
                    .execute(args);
            Object obj = res.as(Object.class);
            // we have to manually serialize because if we return obj here, the GraalJS context would be closed when Quarkus tries to serialize it
            return objectMapper.writeValueAsString(obj);
        }
    }

    private String resourceAsString(String resourceName) throws IOException {
        try (var resourceStream = GreetingResource.class.getResourceAsStream(resourceName)) {
            var bytes = Objects.requireNonNull(resourceStream).readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    public record Data(String msg) {

    }
}
