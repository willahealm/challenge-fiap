package br.com.fiap.embarquefacil.data;

import android.os.Handler;
import android.os.Looper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import br.com.fiap.embarquefacil.data.model.ApiMessage;
import br.com.fiap.embarquefacil.data.model.AuthSession;
import br.com.fiap.embarquefacil.data.model.Handoff;
import br.com.fiap.embarquefacil.data.model.Journey;
import br.com.fiap.embarquefacil.data.model.JourneyDetails;
import br.com.fiap.embarquefacil.data.model.OperationalAlert;
import br.com.fiap.embarquefacil.data.model.Route;
import br.com.fiap.embarquefacil.data.model.RouteStep;
import br.com.fiap.embarquefacil.data.model.Trip;
import br.com.fiap.embarquefacil.data.model.User;
import br.com.fiap.embarquefacil.util.DemoScenario;

public class DemoRepository implements JourneyRepository {
    private final SessionStore store;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final JourneyDetails details;
    private int journeyLoads;

    public DemoRepository(SessionStore store) {
        this.store = store;
        details = createDemoJourney();
    }

    @Override public void login(String email, String password, ResultCallback<AuthSession> callback) {
        later(() -> {
            User user = new User();
            user.id = "passenger-lucas";
            user.name = "Lucas";
            user.email = email;
            user.role = "PASSENGER";
            callback.onSuccess(new AuthSession("demo-local-session", user));
        });
    }

    @Override public void logout(ResultCallback<ApiMessage> callback) {
        later(() -> callback.onSuccess(message("Sessão encerrada.")));
    }

    @Override public void nextTrip(ResultCallback<Trip> callback) {
        later(() -> callback.onSuccess(details.trip));
    }

    @Override public void journey(String tripId, ResultCallback<JourneyDetails> callback) {
        journeyLoads++;
        if (DemoScenario.shouldPublishPlatformChange(journeyLoads) && details.alerts.isEmpty()) {
            details.trip.platform = "21";
            OperationalAlert alert = new OperationalAlert();
            alert.id = "alert-platform-demo";
            alert.type = "PLATFORM_CHANGED";
            alert.severity = "CRITICAL";
            alert.platform = "21";
            alert.message = "Plataforma alterada: antes 18, agora 21. Siga as placas do setor C.";
            alert.createdAt = Instant.now().toString();
            details.alerts.add(alert);
            if (details.route != null && !details.route.steps.isEmpty()) {
                details.route.destination = "Plataforma 21";
                details.route.steps.get(0).title = "Corredor do setor C";
                details.route.steps.get(0).instruction = "Vire à direita e siga as placas azuis do setor C.";
                details.route.steps.get(0).headingDegrees = 92f;
                if (details.route.steps.size() > 1) {
                    details.route.steps.get(1).title = "Painel do setor C";
                    details.route.steps.get(1).instruction = "Procure no painel do setor C a indicação da plataforma 21.";
                }
                RouteStep arrival = details.route.steps.get(details.route.steps.size() - 1);
                arrival.title = "Plataforma 21";
                arrival.instruction = "Siga pelo corredor até a plataforma 21.";
            }
        }
        later(() -> callback.onSuccess(details));
    }

    @Override public void updateChecklist(String journeyId, Map<String, Boolean> checklist, ResultCallback<ApiMessage> callback) {
        // The ViewModel may pass the same map held by details; copy before
        // updating it so saving does not erase the checked items.
        details.journey.checklist = new LinkedHashMap<>(checklist);
        for (Map.Entry<String, Boolean> item : checklist.entrySet()) {
            store.preferences().edit().putBoolean("demo_check_" + item.getKey(), item.getValue()).apply();
        }
        later(() -> callback.onSuccess(message("Checklist salvo no aparelho.")));
    }

    @Override public void validateCheckpoint(String journeyId, String code, ResultCallback<ApiMessage> callback) {
        if (!DemoScenario.isValidCheckpoint(code)) {
            later(() -> callback.onError("Código não reconhecido. No demo, use TIETE-TOTEM-01."));
            return;
        }
        details.journey.currentPointId = "entrada-setor-a";
        details.journey.currentPointName = "Entrada do setor A";
        details.journey.stage = "AT_TERMINAL";
        later(() -> callback.onSuccess(message("Localização confirmada: Entrada do setor A.")));
    }

    @Override public void createHandoff(String journeyId, ResultCallback<Handoff> callback) {
        Handoff handoff = new Handoff();
        handoff.token = "EF-DEMO-" + UUID.randomUUID().toString();
        handoff.code = "LUCAS1";
        handoff.expiresAt = Instant.now().plusSeconds(60).toString();
        later(() -> callback.onSuccess(handoff));
    }

    @Override public void completeJourney(String journeyId, ResultCallback<ApiMessage> callback) {
        details.journey.stage = "COMPLETED";
        details.trip.status = "EMBARQUE CONCLUÍDO";
        later(() -> callback.onSuccess(message("Chegada confirmada. Boa viagem!")));
    }

    @Override public void submitFeedback(String journeyId, int rating, String tags, String comment, ResultCallback<ApiMessage> callback) {
        later(() -> callback.onSuccess(message("Obrigado pela avaliação de " + rating + " estrelas.")));
    }

    public void reset() {
        journeyLoads = 0;
        JourneyDetails fresh = createDemoJourney();
        details.trip = fresh.trip;
        details.journey = fresh.journey;
        details.route = fresh.route;
        details.alerts = fresh.alerts;
    }

    private JourneyDetails createDemoJourney() {
        JourneyDetails result = new JourneyDetails();
        Trip trip = new Trip();
        trip.id = "trip-demo";
        trip.origin = "São Paulo";
        trip.destination = "Rio de Janeiro";
        trip.departureAt = "14 set • 08:30";
        trip.company = "Viação Cometa";
        trip.platform = "18";
        trip.status = "CONFIRMADA";
        trip.terminal = "Terminal Rodoviário Tietê";
        result.trip = trip;

        Journey journey = new Journey();
        journey.id = "journey-demo";
        journey.tripId = trip.id;
        journey.stage = "PREPARING";
        journey.currentPointName = "Ainda não confirmado";
        journey.checklist.put("document", store.preferences().getBoolean("demo_check_document", false));
        journey.checklist.put("ticket", store.preferences().getBoolean("demo_check_ticket", false));
        journey.checklist.put("luggage", store.preferences().getBoolean("demo_check_luggage", false));
        journey.checklist.put("departureTime", store.preferences().getBoolean("demo_check_departureTime", false));
        result.journey = journey;

        Route route = new Route();
        route.id = "route-tiete-18";
        route.destination = "Plataforma 18";
        route.steps = new ArrayList<>();
        route.steps.add(new RouteStep("step-1", "Escada rolante do setor B", "Siga em frente por 35 m e vire à direita na escada rolante.", 35, 68f));
        route.steps.add(new RouteStep("step-2", "Painel azul", "Suba um piso e procure o painel azul com os números 16–20.", 22, 12f));
        route.steps.add(new RouteStep("step-3", "Plataforma 18", "Siga pelo corredor até a plataforma 18.", 48, 350f));
        result.route = route;
        result.alerts = new ArrayList<>();
        return result;
    }

    private ApiMessage message(String text) {
        ApiMessage result = new ApiMessage();
        result.message = text;
        result.stage = details.journey.stage;
        result.currentPointId = details.journey.currentPointId;
        return result;
    }

    private void later(Runnable runnable) {
        handler.postDelayed(runnable, 250);
    }
}
