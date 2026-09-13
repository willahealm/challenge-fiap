package br.com.fiap.embarquefacil.data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import br.com.fiap.embarquefacil.BuildConfig;
import br.com.fiap.embarquefacil.data.model.ApiMessage;
import br.com.fiap.embarquefacil.data.model.AuthSession;
import br.com.fiap.embarquefacil.data.model.Handoff;
import br.com.fiap.embarquefacil.data.model.JourneyDetails;
import br.com.fiap.embarquefacil.data.model.Feedback;
import br.com.fiap.embarquefacil.data.model.Trip;
import br.com.fiap.embarquefacil.data.remote.ApiService;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RemoteRepository implements JourneyRepository {
    private final SessionStore store;
    private final ApiService api;

    public RemoteRepository(SessionStore store) {
        this.store = store;
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BASIC : HttpLoggingInterceptor.Level.NONE);
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    okhttp3.Request.Builder builder = chain.request().newBuilder()
                            .header("Accept", "application/json");
                    AuthSession session = store.getSession();
                    if (session != null && session.accessToken != null) {
                        builder.header("Authorization", "Bearer " + session.accessToken);
                    }
                    return chain.proceed(builder.build());
                })
                .addInterceptor(logging)
                .build();
        api = new Retrofit.Builder()
                .baseUrl(store.getBaseUrl())
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService.class);
    }

    @Override public void login(String email, String password, ResultCallback<AuthSession> callback) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", password);
        enqueue(api.login(body), callback);
    }

    @Override public void logout(ResultCallback<ApiMessage> callback) {
        api.logout().enqueue(new Callback<>() {
            @Override public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    ApiMessage message = new ApiMessage();
                    message.message = "Sessão encerrada.";
                    callback.onSuccess(message);
                } else callback.onError("A sessão local foi encerrada, mas a API respondeu HTTP " + response.code() + ".");
            }
            @Override public void onFailure(Call<Map<String, String>> call, Throwable throwable) {
                callback.onError("A sessão local foi encerrada; não foi possível avisar a API.");
            }
        });
    }

    @Override public void nextTrip(ResultCallback<Trip> callback) {
        enqueue(api.nextTrip(), callback);
    }

    @Override public void journey(String tripId, ResultCallback<JourneyDetails> callback) {
        enqueue(api.journey(tripId), callback);
    }

    @Override public void updateChecklist(String journeyId, Map<String, Boolean> checklist, ResultCallback<ApiMessage> callback) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("checklist", checklist);
        enqueueMessage(api.updateChecklist(journeyId, body), "Checklist salvo.", callback);
    }

    @Override public void validateCheckpoint(String journeyId, String code, ResultCallback<ApiMessage> callback) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("code", code);
        enqueueMessage(api.validateCheckpoint(journeyId, body), "Localização confirmada.", callback);
    }

    @Override public void createHandoff(String journeyId, ResultCallback<Handoff> callback) {
        enqueue(api.createHandoff(journeyId), callback);
    }

    @Override public void completeJourney(String journeyId, ResultCallback<ApiMessage> callback) {
        enqueueMessage(api.completeJourney(journeyId), "Chegada confirmada. Boa viagem!", callback);
    }

    @Override public void submitFeedback(String journeyId, int rating, String tags, String comment, ResultCallback<ApiMessage> callback) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("journeyId", journeyId);
        body.put("rating", rating);
        body.put("tags", tags == null || tags.isBlank() ? java.util.Collections.emptyList() : java.util.Collections.singletonList(tags));
        body.put("comment", comment == null ? "" : comment);
        api.submitFeedback(body).enqueue(new Callback<>() {
            @Override public void onResponse(Call<Feedback> call, Response<Feedback> response) {
                if (response.isSuccessful()) {
                    ApiMessage message = new ApiMessage();
                    message.message = "Obrigado pela sua avaliação.";
                    callback.onSuccess(message);
                } else callback.onError("Não foi possível enviar a avaliação (HTTP " + response.code() + ").");
            }
            @Override public void onFailure(Call<Feedback> call, Throwable throwable) {
                callback.onError("API indisponível. A avaliação não foi enviada.");
            }
        });
    }

    private void enqueueMessage(Call<JourneyDetails> call, String successText, ResultCallback<ApiMessage> callback) {
        call.enqueue(new Callback<>() {
            @Override public void onResponse(Call<JourneyDetails> call, Response<JourneyDetails> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiMessage message = new ApiMessage();
                    message.message = successText;
                    message.stage = response.body().journey == null ? null : response.body().journey.stage;
                    message.currentPointId = response.body().journey == null ? null : response.body().journey.currentPointId;
                    callback.onSuccess(message);
                } else callback.onError("Não foi possível concluir (HTTP " + response.code() + ").");
            }
            @Override public void onFailure(Call<JourneyDetails> call, Throwable throwable) {
                callback.onError("API indisponível. Confira a URL ou ative o modo demonstração.");
            }
        });
    }

    private <T> void enqueue(Call<T> call, ResultCallback<T> callback) {
        call.enqueue(new Callback<>() {
            @Override public void onResponse(Call<T> call, Response<T> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }
                String message = "Não foi possível concluir (HTTP " + response.code() + ").";
                try {
                    if (response.errorBody() != null) {
                        JsonObject error = new Gson().fromJson(response.errorBody().string(), JsonObject.class);
                        if (error != null && error.has("message")) message = error.get("message").getAsString();
                    }
                } catch (IOException | RuntimeException ignored) { }
                callback.onError(message);
            }

            @Override public void onFailure(Call<T> call, Throwable throwable) {
                callback.onError("API indisponível. Confira a URL ou ative o modo demonstração.");
            }
        });
    }
}
