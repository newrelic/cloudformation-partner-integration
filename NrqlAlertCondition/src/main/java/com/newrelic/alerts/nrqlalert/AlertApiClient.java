package com.newrelic.alerts.nrqlalert;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.alerts.nrqlalert.model.NewRelicNrqlCondition;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Abstracted client that handles all http interactions.
 */
public class AlertApiClient {

    public static final String USER_AGENT = "User-Agent";
    public static final String X_API_KEY = "X-Api-Key";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String URL_PREFIX = "https://api.newrelic.com/v2/alerts_nrql_conditions";

    private String url;
    private HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();


    public AlertApiClient(String url, HttpClient httpClient) {
        this.url = url;
        this.httpClient = httpClient;
    }

    public JSONObject create(NewRelicNrqlCondition newRelicNrqlCondition, String apiKey, int policyId) throws IOException, AlertApiException {
        HttpPost alertPost = new HttpPost(
                String.format("%s/policies/%d.json", this.url, policyId)
        );
        alertPost.addHeader(X_API_KEY, apiKey);
        alertPost.addHeader(USER_AGENT, "NRLambdaResourceProvider");
        alertPost.addHeader(CONTENT_TYPE, "application/json");
        alertPost.setEntity(new StringEntity(mapper.writeValueAsString(Collections.singletonMap("nrql_condition", newRelicNrqlCondition))));

        HttpResponse response = this.httpClient.execute(alertPost);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode >= 400) {
            JsonNode jsonNode = mapper.readTree(response.getEntity().getContent());
            String message = jsonNode.findValue("title").asText();
            // Consume entity to avoid leaking the connection
            EntityUtils.consume(response.getEntity());
            throw new AlertApiException(String.format("Failed to create alert condition for policy ID: %d. Error message: '%s'", policyId, message));
        } else {
            String jsonResponse = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            JSONObject alertResponse = new JSONObject(jsonResponse);
            return alertResponse.getJSONObject("nrql_condition");
        }
    }

    public JSONObject update(NewRelicNrqlCondition newRelicNrqlCondition, String apiKey, int conditionId) throws IOException, AlertApiException {
        HttpPut alertPut = new HttpPut(
                String.format("%s/%d.json", this.url, conditionId)
        );
        alertPut.addHeader(X_API_KEY, apiKey);
        alertPut.addHeader(USER_AGENT, "NRLambdaResourceProvider");
        alertPut.addHeader(CONTENT_TYPE, "application/json");
        alertPut.setEntity(new StringEntity(mapper.writeValueAsString(Collections.singletonMap("nrql_condition", newRelicNrqlCondition))));

        HttpResponse response = this.httpClient.execute(alertPut);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode >= 400) {
            JsonNode jsonNode = mapper.readTree(response.getEntity().getContent());
            String message = jsonNode.findValue("title").asText();
            // Consume entity to avoid leaking the connection
            EntityUtils.consume(response.getEntity());
            throw new AlertApiException(String.format("Failed to update alert condition for condition ID: %d. Error message: '%s'", conditionId, message));
        } else {
            String jsonResponse = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            JSONObject alertResponse = new JSONObject(jsonResponse);
            return alertResponse.getJSONObject("nrql_condition");
        }
    }

    public void remove(String apiKey, int conditionId) throws AlertApiException, IOException {
        HttpDelete delete = new HttpDelete(
                String.format("%s/%d.json", url, conditionId)
        );
        delete.addHeader(X_API_KEY, apiKey);
        delete.addHeader(USER_AGENT, "NRLambdaResourceProvider");

        HttpResponse response = this.httpClient.execute(delete);
        int statusCode = response.getStatusLine().getStatusCode();
        // 404 isn't really a failure; the entity doesn't exist, so the post condition is true
        if (statusCode >= 400 && statusCode != 404) {
            EntityUtils.consume(response.getEntity());

            if (statusCode == 401) {
                throw new AlertApiException(String.format("Unauthorized. Failed to delete alert condition with ID: %d. Probably an invalid API key.", conditionId));
            } else {
                throw new AlertApiException(String.format("Failed to delete alert condition with ID: %d", conditionId));
            }
        }
    }
}
