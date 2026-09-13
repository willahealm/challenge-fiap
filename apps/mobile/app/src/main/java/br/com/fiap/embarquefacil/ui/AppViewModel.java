package br.com.fiap.embarquefacil.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Map;

import br.com.fiap.embarquefacil.EasyBoardingApp;
import br.com.fiap.embarquefacil.data.AppRepository;
import br.com.fiap.embarquefacil.data.ResultCallback;
import br.com.fiap.embarquefacil.data.SessionStore;
import br.com.fiap.embarquefacil.data.model.ApiMessage;
import br.com.fiap.embarquefacil.data.model.AuthSession;
import br.com.fiap.embarquefacil.data.model.Handoff;
import br.com.fiap.embarquefacil.data.model.JourneyDetails;
import br.com.fiap.embarquefacil.data.model.OperationalAlert;
import br.com.fiap.embarquefacil.data.model.Trip;

public class AppViewModel extends AndroidViewModel {
    private final AppRepository repository;
    private final SessionStore sessionStore;

    private final MutableLiveData<AuthSession> session = new MutableLiveData<>();
    private final MutableLiveData<UiState<AuthSession>> loginState = new MutableLiveData<>(UiState.idle());
    private final MutableLiveData<UiState<Trip>> tripState = new MutableLiveData<>(UiState.idle());
    private final MutableLiveData<UiState<JourneyDetails>> journeyState = new MutableLiveData<>(UiState.idle());
    private final MutableLiveData<UiState<Handoff>> handoffState = new MutableLiveData<>(UiState.idle());
    private final MutableLiveData<Event<String>> messageEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<OperationalAlert>> alertEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> checkpointEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> completedEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> feedbackEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> logoutEvent = new MutableLiveData<>();
    private String lastAlertId;

    public AppViewModel(@NonNull Application application) {
        super(application);
        EasyBoardingApp app = (EasyBoardingApp) application;
        repository = app.repository();
        sessionStore = app.sessionStore();
        session.setValue(sessionStore.getSession());
    }

    public LiveData<AuthSession> session() { return session; }
    public LiveData<UiState<AuthSession>> loginState() { return loginState; }
    public LiveData<UiState<Trip>> tripState() { return tripState; }
    public LiveData<UiState<JourneyDetails>> journeyState() { return journeyState; }
    public LiveData<UiState<Handoff>> handoffState() { return handoffState; }
    public LiveData<Event<String>> messageEvent() { return messageEvent; }
    public LiveData<Event<OperationalAlert>> alertEvent() { return alertEvent; }
    public LiveData<Event<Boolean>> checkpointEvent() { return checkpointEvent; }
    public LiveData<Event<Boolean>> completedEvent() { return completedEvent; }
    public LiveData<Event<Boolean>> feedbackEvent() { return feedbackEvent; }
    public LiveData<Event<Boolean>> logoutEvent() { return logoutEvent; }

    public SessionStore settings() { return sessionStore; }

    public void login(String email, String password) {
        loginState.setValue(UiState.loading(null));
        repository.login(email, password, new ResultCallback<>() {
            @Override public void onSuccess(AuthSession value) {
                sessionStore.saveSession(value, email);
                session.setValue(value);
                loginState.setValue(UiState.success(value));
            }
            @Override public void onError(String message) { loginState.setValue(UiState.error(message, null)); }
        });
    }

    public void logout() {
        repository.logout(new ResultCallback<>() {
            @Override public void onSuccess(ApiMessage value) { finishLogout(null); }
            @Override public void onError(String message) { finishLogout(message); }
        });
    }

    public void logoutLocal() {
        finishLogout(null);
    }

    private void finishLogout(String warning) {
        sessionStore.clearSession();
        session.setValue(null);
        tripState.setValue(UiState.idle());
        journeyState.setValue(UiState.idle());
        lastAlertId = null;
        if (warning != null) messageEvent.setValue(new Event<>(warning));
        logoutEvent.setValue(new Event<>(true));
    }

    public void loadNextTrip() {
        Trip existing = tripState.getValue() == null ? null : tripState.getValue().data;
        tripState.setValue(UiState.loading(existing));
        repository.nextTrip(new ResultCallback<>() {
            @Override public void onSuccess(Trip value) { tripState.setValue(UiState.success(value)); }
            @Override public void onError(String message) { tripState.setValue(UiState.error(message, existing)); }
        });
    }

    public void loadJourney(boolean showLoading) {
        UiState<JourneyDetails> current = journeyState.getValue();
        JourneyDetails existing = current == null ? null : current.data;
        String tripId = existing != null && existing.trip != null ? existing.trip.id : currentTripId();
        if (tripId == null) {
            journeyState.setValue(UiState.error("Abra uma viagem antes de iniciar a jornada.", existing));
            return;
        }
        if (showLoading) journeyState.setValue(UiState.loading(existing));
        repository.journey(tripId, new ResultCallback<>() {
            @Override public void onSuccess(JourneyDetails value) {
                OperationalAlert alert = value.latestAlert();
                if (alert != null && alert.id != null && !alert.id.equals(lastAlertId)) {
                    lastAlertId = alert.id;
                    alertEvent.setValue(new Event<>(alert));
                }
                journeyState.setValue(UiState.success(value));
                if (value.trip != null) tripState.setValue(UiState.success(value.trip));
            }
            @Override public void onError(String message) {
                journeyState.setValue(UiState.error(message, existing));
            }
        });
    }

    public void updateChecklist(Map<String, Boolean> checklist) {
        JourneyDetails details = currentJourney();
        if (details == null || details.journey == null) return;
        details.journey.checklist = checklist;
        journeyState.setValue(UiState.success(details));
        repository.updateChecklist(details.journey.id, checklist, messageCallback(true));
    }

    public void validateCheckpoint(String code) {
        JourneyDetails details = currentJourney();
        if (details == null || details.journey == null) {
            messageEvent.setValue(new Event<>("Jornada não carregada."));
            return;
        }
        repository.validateCheckpoint(details.journey.id, code, new ResultCallback<>() {
            @Override public void onSuccess(ApiMessage value) {
                messageEvent.setValue(new Event<>(value.message));
                checkpointEvent.setValue(new Event<>(true));
                loadJourney(false);
            }
            @Override public void onError(String message) {
                messageEvent.setValue(new Event<>(message));
                checkpointEvent.setValue(new Event<>(false));
            }
        });
    }

    public void createHandoff() {
        JourneyDetails details = currentJourney();
        if (details == null || details.journey == null) return;
        handoffState.setValue(UiState.loading(null));
        repository.createHandoff(details.journey.id, new ResultCallback<>() {
            @Override public void onSuccess(Handoff value) { handoffState.setValue(UiState.success(value)); }
            @Override public void onError(String message) { handoffState.setValue(UiState.error(message, null)); }
        });
    }

    public void completeJourney() {
        JourneyDetails details = currentJourney();
        if (details == null || details.journey == null) return;
        repository.completeJourney(details.journey.id, new ResultCallback<>() {
            @Override public void onSuccess(ApiMessage value) {
                messageEvent.setValue(new Event<>(value.message));
                completedEvent.setValue(new Event<>(true));
            }
            @Override public void onError(String message) { messageEvent.setValue(new Event<>(message)); }
        });
    }

    public void submitFeedback(int rating, String tags, String comment) {
        JourneyDetails details = currentJourney();
        if (details == null || details.journey == null) return;
        repository.submitFeedback(details.journey.id, rating, tags, comment, new ResultCallback<>() {
            @Override public void onSuccess(ApiMessage value) {
                messageEvent.setValue(new Event<>(value.message));
                feedbackEvent.setValue(new Event<>(true));
            }
            @Override public void onError(String message) { messageEvent.setValue(new Event<>(message)); }
        });
    }

    public void resetDemo() {
        repository.resetDemo();
        lastAlertId = null;
        loadNextTrip();
    }

    public JourneyDetails currentJourney() {
        UiState<JourneyDetails> state = journeyState.getValue();
        return state == null ? null : state.data;
    }

    private String currentTripId() {
        UiState<Trip> state = tripState.getValue();
        return state != null && state.data != null ? state.data.id : null;
    }

    private ResultCallback<ApiMessage> messageCallback(boolean reload) {
        return new ResultCallback<>() {
            @Override public void onSuccess(ApiMessage value) {
                if (value != null && value.message != null) messageEvent.setValue(new Event<>(value.message));
                if (reload) loadJourney(false);
            }
            @Override public void onError(String message) { messageEvent.setValue(new Event<>(message)); }
        };
    }
}
