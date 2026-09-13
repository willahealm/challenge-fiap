package br.com.fiap.embarquefacil.data.remote;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import br.com.fiap.embarquefacil.BuildConfig;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

public final class AppwriteGatewayInterceptor implements Interceptor {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final Gson gson = new Gson();

    @Override public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        if (!BuildConfig.USE_APPWRITE_GATEWAY) return chain.proceed(original);

        HttpUrl endpoint = HttpUrl.get(BuildConfig.APPWRITE_ENDPOINT);
        String path = original.url().encodedPath();
        String endpointPath = endpoint.encodedPath().replaceAll("/$", "");
        if (!endpointPath.isEmpty() && path.startsWith(endpointPath + "/")) {
            path = path.substring(endpointPath.length());
        }
        if (!path.startsWith("/")) path = "/" + path;

        JsonObject forwardedHeaders = new JsonObject();
        for (String name : original.headers().names()) {
            if (!name.equalsIgnoreCase("host") && !name.equalsIgnoreCase("content-length")) {
                forwardedHeaders.addProperty(name.toLowerCase(), original.header(name));
            }
        }

        String requestBody = "";
        if (original.body() != null) {
            Buffer buffer = new Buffer();
            original.body().writeTo(buffer);
            requestBody = buffer.readUtf8();
        }

        JsonObject execution = new JsonObject();
        execution.addProperty("async", false);
        execution.addProperty("path", path);
        execution.addProperty("method", original.method());
        execution.add("headers", forwardedHeaders);
        execution.addProperty("body", requestBody);

        HttpUrl gatewayUrl = endpoint.newBuilder()
                .addPathSegment("functions")
                .addPathSegment(BuildConfig.APPWRITE_FUNCTION_ID)
                .addPathSegment("executions")
                .build();
        Request gatewayRequest = new Request.Builder()
                .url(gatewayUrl)
                .post(RequestBody.create(gson.toJson(execution), JSON))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("X-Appwrite-Project", BuildConfig.APPWRITE_PROJECT_ID)
                .build();

        Response gatewayResponse = chain.proceed(gatewayRequest);
        if (!gatewayResponse.isSuccessful() || gatewayResponse.body() == null) return gatewayResponse;

        String envelopeText = gatewayResponse.body().string();
        JsonObject envelope = JsonParser.parseString(envelopeText).getAsJsonObject();
        int status = envelope.has("responseStatusCode") ? envelope.get("responseStatusCode").getAsInt() : 500;
        String body = envelope.has("responseBody") && !envelope.get("responseBody").isJsonNull()
                ? envelope.get("responseBody").getAsString() : "";
        Headers.Builder responseHeaders = new Headers.Builder();
        JsonArray headers = envelope.has("responseHeaders") ? envelope.getAsJsonArray("responseHeaders") : new JsonArray();
        for (JsonElement item : headers) {
            JsonObject header = item.getAsJsonObject();
            if (header.has("name") && header.has("value")) {
                responseHeaders.add(header.get("name").getAsString(), header.get("value").getAsString());
            }
        }
        MediaType contentType = MediaType.get("application/json; charset=utf-8");
        String declaredType = responseHeaders.get("content-type");
        if (declaredType != null) {
            MediaType parsed = MediaType.parse(declaredType);
            if (parsed != null) contentType = parsed;
        }
        return gatewayResponse.newBuilder()
                .request(original)
                .code(status)
                .message("Appwrite Function HTTP " + status)
                .headers(responseHeaders.build())
                .body(ResponseBody.create(body, contentType))
                .build();
    }
}
