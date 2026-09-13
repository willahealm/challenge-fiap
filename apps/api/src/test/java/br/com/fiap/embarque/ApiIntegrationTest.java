package br.com.fiap.embarque;

import static br.com.fiap.embarque.Models.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest @AutoConfigureMockMvc
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired DemoAuth auth;
    @Autowired JourneyService service;
    @Autowired ObjectMapper json;
    String passenger,operator,device;
    @BeforeEach void setup() {
        var ops=new Actor("operator-demo","Operador","OPERATOR",null,null);service.reset(ops);
        passenger=token(new Actor("user-lucas","Lucas","PASSENGER",null,null));operator=token(ops);
        device=token(new Actor("device-demo","Totem","TOTEM",null,"totem-tiete-01"));
    }
    String token(Actor actor) { return "Bearer "+auth.issue(actor,Duration.ofHours(1)).accessToken(); }
    @Test void healthIsPublicAndDataRequiresAuthentication() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/v1/ops/trips")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(get("/v1/me/trips/next").header("Authorization","Bearer bogus")).andExpect(status().isUnauthorized());
    }
    @Test void loginValidatesCredentialsAndLogoutRevokesToken() throws Exception {
        mvc.perform(post("/v1/auth/demo").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"lucas@demo.local\",\"password\":\"wrong\"}")).andExpect(status().isUnauthorized());
        var result=mvc.perform(post("/v1/auth/demo").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"lucas@demo.local\",\"password\":\"Demo123!\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("PASSENGER")).andReturn();
        String token="Bearer "+json.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        mvc.perform(delete("/v1/auth/session").header("Authorization",token)).andExpect(status().isOk());
        mvc.perform(get("/v1/me/trips/next").header("Authorization",token)).andExpect(status().isUnauthorized());
    }
    @Test void rolesAndOwnershipAreEnforced() throws Exception {
        mvc.perform(get("/v1/trips/trip-ana/journey").header("Authorization",passenger)).andExpect(status().isForbidden());
        mvc.perform(get("/v1/ops/trips").header("Authorization",passenger)).andExpect(status().isForbidden());
        mvc.perform(get("/v1/me/trips/next").header("Authorization",device)).andExpect(status().isForbidden());
        mvc.perform(get("/v1/trips/trip-demo/journey").header("Authorization",device)).andExpect(status().isForbidden());
        mvc.perform(post("/v1/ops/trips/trip-demo/alerts").header("Authorization",passenger).contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"INFO\",\"severity\":\"INFO\",\"message\":\"test\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/v1/ops/trips").header("Authorization",operator)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }
    @Test void fullPassengerOperatorKioskSlice() throws Exception {
        mvc.perform(get("/v1/me/trips/next").header("Authorization",passenger)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value("trip-demo"));
        mvc.perform(patch("/v1/journeys/journey-demo/checklist").header("Authorization",passenger).contentType(MediaType.APPLICATION_JSON).content("{\"checklist\":{\"document\":true}}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.journey.checklist.document").value(true));
        var handoff=mvc.perform(post("/v1/journeys/journey-demo/handoffs").header("Authorization",passenger)).andExpect(status().isOk()).andReturn();
        String code=json.readTree(handoff.getResponse().getContentAsString()).get("token").asText();
        var consume=mvc.perform(post("/v1/totems/totem-tiete-01/handoffs/consume").header("Authorization",device).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(new ConsumeRequest(code))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.journey.currentPointId").value("point-totem-01")).andReturn();
        String kiosk="Bearer "+json.readTree(consume.getResponse().getContentAsString()).get("accessToken").asText();
        mvc.perform(post("/v1/totems/totem-tiete-01/help-requests").header("Authorization",kiosk).contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"MOBILITY\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.tripId").value("trip-demo"));
        mvc.perform(post("/v1/ops/trips/trip-demo/alerts").header("Authorization",operator).contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"PLATFORM_CHANGE\",\"severity\":\"CRITICAL\",\"message\":\"Vá à plataforma 21\",\"platform\":\"21\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.previousPlatform").value("18"));
        mvc.perform(get("/v1/trips/trip-demo/journey").header("Authorization",kiosk)).andExpect(status().isOk()).andExpect(jsonPath("$.route.toPointId").value("point-platform-21")).andExpect(jsonPath("$.alerts.length()").value(1));
        mvc.perform(post("/v1/journeys/journey-demo/complete").header("Authorization",passenger)).andExpect(status().isOk()).andExpect(jsonPath("$.journey.stage").value("COMPLETED"));
        mvc.perform(post("/v1/feedback").header("Authorization",passenger).contentType(MediaType.APPLICATION_JSON).content("{\"journeyId\":\"journey-demo\",\"rating\":5}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.rating").value(5));
    }
    @Test void invalidPayloadsFailBeforeMutation() throws Exception {
        mvc.perform(post("/v1/auth/demo").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"lucas@demo.local\",\"password\":\"Demo123!\",\"role\":\"OPERATOR\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/v1/feedback").header("Authorization",passenger).contentType(MediaType.APPLICATION_JSON).content("{\"journeyId\":\"journey-demo\",\"rating\":5,\"tags\":[null]}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/v1/ops/trips/trip-demo/alerts").header("Authorization",operator).contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"PLATFORM_CHANGE\",\"severity\":\"CRITICAL\",\"message\":\"Mudou\",\"platform\":\"99\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mvc.perform(patch("/v1/journeys/journey-demo/checklist").header("Authorization",passenger).contentType(MediaType.APPLICATION_JSON).content("{\"checklist\":{\"document\":null}}"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/v1/trips/trip-demo/journey").header("Authorization",passenger)).andExpect(jsonPath("$.trip.platform").value("18"));
    }
    @Test void corsPermitsLocalClientsAndRejectsUnknownOrigins() throws Exception {
        mvc.perform(options("/v1/ops/trips").header("Origin","http://localhost:5173").header("Access-Control-Request-Method","GET").header("Access-Control-Request-Headers","Authorization"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:5173"));
        mvc.perform(options("/v1/ops/trips").header("Origin","https://untrusted.example").header("Access-Control-Request-Method","GET")).andExpect(status().isForbidden());
    }
}
