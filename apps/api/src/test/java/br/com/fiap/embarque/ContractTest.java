package br.com.fiap.embarque;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

/** Guards the independently maintained clients' contract against server DTO/route drift. */
class ContractTest {
    final ObjectMapper json=new ObjectMapper();
    JsonNode contract() throws Exception { return json.readTree(Path.of(System.getProperty("contracts.path"),"openapi.json").toFile()); }
    @Test void everyPublicDtoMatchesOpenApiAndStandaloneSchema() throws Exception {
        var api=contract();var schemas=api.path("components").path("schemas");
        var standalone=json.readTree(Path.of(System.getProperty("contracts.path"),"schemas/v1.schema.json").toFile()).path("$defs");
        var aliases=Map.of("Login","LoginRequest");
        for(var type:Models.class.getDeclaredClasses()) {
            if(type.getSimpleName().equals("Actor") || type.getSimpleName().equals("Totem")) continue;
            var name=aliases.getOrDefault(type.getSimpleName(),type.getSimpleName());
            var schema=schemas.path(name);assertThat(schema.isMissingNode()).as(name).isFalse();
            var fields=new ArrayList<String>();schema.path("properties").fieldNames().forEachRemaining(fields::add);
            assertThat(fields).as(name).containsExactlyInAnyOrder(Arrays.stream(type.getRecordComponents()).map(c -> c.getName()).toArray(String[]::new));
            assertThat(standalone.path(name)).isEqualTo(json.readTree(schema.toString().replace("#/components/schemas/","#/$defs/")));
        }
    }
    @Test void allControllerMethodsHaveMatchingPathsAndHttpVerbs() throws Exception {
        var paths=contract().path("paths");int endpoints=0;
        for(var method:ApiController.class.getDeclaredMethods()) {
            String verb=null,path=null;
            if(method.isAnnotationPresent(GetMapping.class)) { verb="get";path=method.getAnnotation(GetMapping.class).value()[0]; }
            if(method.isAnnotationPresent(PostMapping.class)) { verb="post";path=method.getAnnotation(PostMapping.class).value()[0]; }
            if(method.isAnnotationPresent(PatchMapping.class)) { verb="patch";path=method.getAnnotation(PatchMapping.class).value()[0]; }
            if(method.isAnnotationPresent(DeleteMapping.class)) { verb="delete";path=method.getAnnotation(DeleteMapping.class).value()[0]; }
            if(verb!=null) { endpoints++;assertThat(paths.path("/v1"+path).path(verb).isMissingNode()).as(verb+" "+path).isFalse(); }
        }
        assertThat(paths.size()).isEqualTo(endpoints+1); // actuator health is outside our controller
    }
    @Test void everySchemaReferenceResolves() throws Exception {
        var api=contract();validateReferences(api,api);
    }
    private void validateReferences(JsonNode node,JsonNode root) {
        if(node.isObject() && node.has("$ref")) {
            String ref=node.get("$ref").asText();assertThat(ref).startsWith("#/");
            assertThat(root.at(ref.substring(1)).isMissingNode()).as(ref).isFalse();
        }
        if(node.isContainerNode()) node.elements().forEachRemaining(child -> validateReferences(child,root));
    }
}
