package br.com.fiap.embarquefacil.data;

import java.util.Map;

import br.com.fiap.embarquefacil.data.model.ApiMessage;
import br.com.fiap.embarquefacil.data.model.AuthSession;
import br.com.fiap.embarquefacil.data.model.Handoff;
import br.com.fiap.embarquefacil.data.model.JourneyDetails;
import br.com.fiap.embarquefacil.data.model.Trip;

public class AppRepository implements JourneyRepository {
    private final SessionStore store;
    private DemoRepository demo;
    private RemoteRepository remote;
    private String remoteUrl;

    public AppRepository(SessionStore store) {
        this.store = store;
        this.demo = new DemoRepository(store);
    }

    private JourneyRepository active() {
        if (store.isDemoMode()) return demo;
        if (remote == null || !store.getBaseUrl().equals(remoteUrl)) {
            remoteUrl = store.getBaseUrl();
            remote = new RemoteRepository(store);
        }
        return remote;
    }

    public void resetDemo() { demo.reset(); }

    @Override public void login(String email, String password, ResultCallback<AuthSession> callback) { active().login(email, password, callback); }
    @Override public void logout(ResultCallback<ApiMessage> callback) { active().logout(callback); }
    @Override public void nextTrip(ResultCallback<Trip> callback) { active().nextTrip(callback); }
    @Override public void journey(String tripId, ResultCallback<JourneyDetails> callback) { active().journey(tripId, callback); }
    @Override public void updateChecklist(String journeyId, Map<String, Boolean> checklist, ResultCallback<ApiMessage> callback) { active().updateChecklist(journeyId, checklist, callback); }
    @Override public void validateCheckpoint(String journeyId, String code, ResultCallback<ApiMessage> callback) { active().validateCheckpoint(journeyId, code, callback); }
    @Override public void createHandoff(String journeyId, ResultCallback<Handoff> callback) { active().createHandoff(journeyId, callback); }
    @Override public void completeJourney(String journeyId, ResultCallback<ApiMessage> callback) { active().completeJourney(journeyId, callback); }
    @Override public void submitFeedback(String journeyId, int rating, String tags, String comment, ResultCallback<ApiMessage> callback) { active().submitFeedback(journeyId, rating, tags, comment, callback); }
}
