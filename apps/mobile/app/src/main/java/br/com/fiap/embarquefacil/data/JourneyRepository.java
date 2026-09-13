package br.com.fiap.embarquefacil.data;

import java.util.Map;

import br.com.fiap.embarquefacil.data.model.ApiMessage;
import br.com.fiap.embarquefacil.data.model.AuthSession;
import br.com.fiap.embarquefacil.data.model.Handoff;
import br.com.fiap.embarquefacil.data.model.JourneyDetails;
import br.com.fiap.embarquefacil.data.model.Trip;

public interface JourneyRepository {
    void login(String email, String password, ResultCallback<AuthSession> callback);
    void logout(ResultCallback<ApiMessage> callback);
    void nextTrip(ResultCallback<Trip> callback);
    void journey(String tripId, ResultCallback<JourneyDetails> callback);
    void updateChecklist(String journeyId, Map<String, Boolean> checklist, ResultCallback<ApiMessage> callback);
    void validateCheckpoint(String journeyId, String code, ResultCallback<ApiMessage> callback);
    void createHandoff(String journeyId, ResultCallback<Handoff> callback);
    void completeJourney(String journeyId, ResultCallback<ApiMessage> callback);
    void submitFeedback(String journeyId, int rating, String tags, String comment, ResultCallback<ApiMessage> callback);
}
