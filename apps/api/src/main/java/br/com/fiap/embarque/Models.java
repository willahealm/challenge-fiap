package br.com.fiap.embarque;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.*;

public final class Models {
    private Models() {}
    public record User(String id, String name, String role) {}
    public record Actor(String id, String name, String role, String journeyId, String totemId) {}
    public record Login(@NotBlank @Email String email, @NotBlank @Size(max=128) String password) {}
    public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, User user) {}
    public record Trip(String id, String terminalId, String origin, String destination, Instant departureAt,
                       String carrier, String platform, String status, int activeJourneys) {}
    public record Journey(String id, String tripId, String userId, String currentPointId, String stage,
                          Map<String, Boolean> checklist, Instant updatedAt) {}
    public record Step(String pointId, String instruction, int distanceMeters, int headingDegrees) {}
    public record Route(String id, String terminalId, String fromPointId, String toPointId, boolean accessible,
                        int distanceMeters, int headingDegrees, List<Step> steps) {}
    public record Alert(String id, String tripId, String type, String severity, String message,
                        String previousPlatform, String platform, Instant createdAt) {}
    public record JourneyView(Journey journey, Trip trip, Route route, List<Alert> alerts) {}
    public record ChecklistRequest(@NotNull @Size(min=1,max=4) Map<String, @NotNull Boolean> checklist) {}
    public record CheckpointRequest(@NotBlank @Size(max=64) String code) {}
    public record HandoffResponse(String token, String code, Instant expiresAt) {}
    public record ConsumeRequest(@NotBlank @Size(max=128) String token) {}
    public record ConsumeResponse(String accessToken, Instant expiresAt, Journey journey, Trip trip, Route route, List<Alert> alerts) {}
    public record AlertRequest(@NotBlank @Pattern(regexp="INFO|PLATFORM_CHANGE|DELAY|BOARDING") String type,
                               @NotBlank @Pattern(regexp="INFO|WARNING|CRITICAL") String severity,
                               @NotBlank @Size(max=500) String message,
                               @Pattern(regexp="18|21") String platform) {}
    public record HelpRequestBody(@Size(max=64) String journeyId,
                                  @NotBlank @Pattern(regexp="MOBILITY|VISION_HEARING|TRIP_INFO|SECURITY") String category) {}
    public record HelpRequest(String id, String journeyId, String tripId, String totemId, String pointId,
                              String category, String status, Instant createdAt) {}
    public record HelpStatusRequest(@NotBlank @Pattern(regexp="ACKNOWLEDGED|RESOLVED") String status) {}
    public record FeedbackRequest(@NotBlank String journeyId, @Min(1) @Max(5) int rating,
                                  @Size(max=5) List<@NotNull @Size(max=40) String> tags, @Size(max=1000) String comment) {}
    public record Feedback(String id, String journeyId, int rating, List<String> tags, String comment, Instant createdAt) {}
    public record Point(String id, String terminalId, String code, String name, String kind) {}
    public record Totem(String id, String terminalId, String pointId, String label, String status) {}
    public record ApiError(String code, String message, Instant timestamp) {}
}
