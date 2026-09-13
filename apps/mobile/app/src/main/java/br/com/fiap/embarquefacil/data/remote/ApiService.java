package br.com.fiap.embarquefacil.data.remote;

import java.util.Map;

import br.com.fiap.embarquefacil.data.model.AuthSession;
import br.com.fiap.embarquefacil.data.model.Feedback;
import br.com.fiap.embarquefacil.data.model.Handoff;
import br.com.fiap.embarquefacil.data.model.JourneyDetails;
import br.com.fiap.embarquefacil.data.model.Trip;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.DELETE;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {
    @POST("v1/auth/demo")
    Call<AuthSession> login(@Body Map<String, String> credentials);

    @DELETE("v1/auth/session")
    Call<Map<String, String>> logout();

    @GET("v1/me/trips/next")
    Call<Trip> nextTrip();

    @GET("v1/trips/{tripId}/journey")
    Call<JourneyDetails> journey(@Path("tripId") String tripId);

    @PATCH("v1/journeys/{journeyId}/checklist")
    Call<JourneyDetails> updateChecklist(@Path("journeyId") String journeyId, @Body Map<String, Object> body);

    @POST("v1/journeys/{journeyId}/checkpoints")
    Call<JourneyDetails> validateCheckpoint(@Path("journeyId") String journeyId, @Body Map<String, String> body);

    @POST("v1/journeys/{journeyId}/handoffs")
    Call<Handoff> createHandoff(@Path("journeyId") String journeyId);

    @POST("v1/journeys/{journeyId}/complete")
    Call<JourneyDetails> completeJourney(@Path("journeyId") String journeyId);

    @POST("v1/feedback")
    Call<Feedback> submitFeedback(@Body Map<String, Object> body);
}
