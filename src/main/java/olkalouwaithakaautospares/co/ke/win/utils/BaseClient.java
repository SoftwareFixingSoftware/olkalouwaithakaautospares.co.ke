package olkalouwaithakaautospares.co.ke.win.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import javax.swing.JOptionPane;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

public class BaseClient {
    private static BaseClient instance;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final String baseUrl;
    private static final CookieManager cookieManager;

    static {
        cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
    }

    private BaseClient() {
        this.httpClient = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .connectTimeout(Duration.ofSeconds(30))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.mapper = new ObjectMapper();
        this.baseUrl = "http://localhost:8080";
    }

    public static BaseClient getInstance() {
        if (instance == null) {
            instance = new BaseClient();
        }
        return instance;
    }

    // Getter for ObjectMapper
    public ObjectMapper getMapper() {
        return mapper;
    }

    public static CookieManager getCookieManager() {
        return cookieManager;
    }

    public static CookieStore getCookieStore() {
        return cookieManager.getCookieStore();
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    // Safe POST method that handles empty responses
    public String post(String endpoint, Object body) throws Exception {
        return safeRequest("POST", endpoint, body);
    }

    // Safe GET method that handles empty responses
    public String get(String endpoint) throws Exception {
        return safeRequest("GET", endpoint, null);
    }

    // Safe PUT method that handles empty responses
    public String put(String endpoint, Object body) throws Exception {
        return safeRequest("PUT", endpoint, body);
    }

    // Safe DELETE method that handles empty responses
    public String delete(String endpoint) throws Exception {
        return safeRequest("DELETE", endpoint, null);
    }

    // Safe request method with better error handling
    private String safeRequest(String method, String endpoint, Object body) throws Exception {
        try {
            String url = baseUrl + endpoint;
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");

            if (body != null) {
                String json = mapper.writeValueAsString(body);
                requestBuilder.method(method, HttpRequest.BodyPublishers.ofString(json));
            } else {
                requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Handle empty responses
            if (response.body() == null || response.body().trim().isEmpty()) {
                return "{}"; // Return empty JSON object instead of empty string
            }

            // Check for session expiration
            if (response.statusCode() == 401) {
                handleSessionExpired();
                throw new Exception("Session expired. Please login again.");
            }

            // Check for other error statuses
            if (response.statusCode() >= 400) {
                String errorMessage = extractErrorMessage(response.body());
                throw new Exception("HTTP " + response.statusCode() + ": " + errorMessage);
            }

            return response.body();

        } catch (Exception e) {
            throw new Exception("Request failed: " + e.getMessage(), e);
        }
    }

    // Extract error message safely
    private String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return "Empty response";
        }
        try {
            JsonNode root = mapper.readTree(responseBody);
            if (root.has("message")) {
                return root.get("message").asText();
            } else if (root.has("error")) {
                return root.get("error").asText();
            } else if (root.has("status")) {
                return "Status: " + root.get("status").asText();
            }
        } catch (Exception e) {
            // If we can't parse JSON, return first 100 chars
            return responseBody.length() > 100 ? responseBody.substring(0, 100) + "..." : responseBody;
        }
        return responseBody;
    }

    // Check if user is authenticated
    public boolean isAuthenticated() {
        try {
            String response = get("/api/auth/me");
            Map<String, Object> result = parseResponse(response);
            return result.get("success") != null && (Boolean) result.get("success");
        } catch (Exception e) {
            return false;
        }
    }

    // Parse response to Map with better error handling
    public Map<String, Object> parseResponse(String jsonResponse) throws Exception {
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            throw new Exception("Empty response received");
        }
        try {
            return mapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new Exception("Failed to parse JSON response: " + e.getMessage() + "\nResponse: " + jsonResponse, e);
        }
    }

    // Parse response to List with robust handling:
    // - Accepts a JSON array: [ {...}, {...} ]
    // - Accepts a wrapped response { "data": [ ... ] }
    // - Accepts a wrapped response where data is a single object { "data": { ... } } -> wraps it into a List
    // - Accepts a single object top-level { ... } -> wraps it into a List
    public List<Map<String, Object>> parseResponseList(String jsonResponse) throws Exception {
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            throw new Exception("Empty response received");
        }
        try {
            // Try parsing directly as a JSON array first
            return mapper.readValue(jsonResponse, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e1) {
            try {
                // Try as a wrapped object: { "data": ... }
                Map<String, Object> responseMap = null;
                try {
                    responseMap = parseResponse(jsonResponse);
                } catch (Exception ex) {
                    // parseResponse failed - maybe top-level is not an object, continue to fallback
                }

                if (responseMap != null && responseMap.containsKey("data")) {
                    Object data = responseMap.get("data");
                    if (data instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> list = (List<Map<String, Object>>) data;
                        return list;
                    } else if (data instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> single = (Map<String, Object>) data;
                        List<Map<String, Object>> wrapped = new ArrayList<>();
                        wrapped.add(single);
                        return wrapped;
                    } else if (data == null) {
                        return new ArrayList<>(); // empty list if data is null
                    }
                }

                // As a last resort: maybe the top-level is a single object that directly represents the item.
                if (responseMap != null) {
                    // Wrap the top-level object as a list (useful when server returns { id:..., name:... })
                    List<Map<String, Object>> wrapped = new ArrayList<>();
                    wrapped.add(responseMap);
                    return wrapped;
                }

                // Nothing matched: rethrow with helpful message
                throw new Exception("Response is not a list or wrapped list: " + jsonResponse);
            } catch (Exception e2) {
                throw new Exception("Failed to parse as list: " + e1.getMessage() + " | " + e2.getMessage(), e2);
            }
        }
    }

    // Get response data object
    public Object getResponseData(String jsonResponse) throws Exception {
        // Prefer extracting "data" from a parsed object, but also handle top-level arrays/objects
        try {
            Map<String, Object> response = parseResponse(jsonResponse);
            if (response.containsKey("data")) {
                return response.get("data");
            } else {
                // No data field: return the parsed object itself
                return response;
            }
        } catch (Exception e) {
            // parseResponse failed: maybe the response is a top-level list
            try {
                List<Map<String, Object>> list = mapper.readValue(jsonResponse, new TypeReference<List<Map<String, Object>>>() {});
                return list;
            } catch (Exception ex) {
                throw new Exception("Failed to extract data: " + e.getMessage() + " | " + ex.getMessage(), ex);
            }
        }
    }

    // Check if response is successful
    public boolean isResponseSuccessful(String jsonResponse) throws Exception {
        Map<String, Object> response = parseResponse(jsonResponse);
        Object success = response.get("success");
        return success != null && (Boolean) success;
    }

    // Get response message
    public String getResponseMessage(String jsonResponse) throws Exception {
        Map<String, Object> response = parseResponse(jsonResponse);
        Object message = response.get("message");
        return message != null ? message.toString() : "";
    }

    // Get response data as list (now consistent)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getResponseDataAsList(String jsonResponse) throws Exception {
        return parseResponseList(jsonResponse);
    }

    // Handle session expired
    private void handleSessionExpired() {
        // Clear cookies
        getCookieStore().removeAll();

        // Show message
        javax.swing.SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null,
                    "Your session has expired. Please login again.",
                    "Session Expired",
                    JOptionPane.WARNING_MESSAGE);
        });
    }

    // Clear session (logout)
    public void logout() throws Exception {
        try {
            // Clear local cookies
            getCookieStore().removeAll();

            // Call logout endpoint
            post("/api/auth/logout", new HashMap<>());
        } catch (Exception e) {
            // Even if server logout fails, clear local session
            getCookieStore().removeAll();
            throw e;
        }
    }

    // Test connection to server
    public boolean testConnection() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/me"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() < 500; // Server is reachable
        } catch (Exception e) {
            return false;
        }
    }
}
