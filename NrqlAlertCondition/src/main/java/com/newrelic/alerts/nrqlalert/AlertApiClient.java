package com.newrelic.alerts.nrqlalert;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.alerts.nrqlalert.model.NewRelicNrqlCondition;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

/** Abstracted client that handles all http interactions. */
public class AlertApiClient {
    public static final String USER_AGENT = "User-Agent";
    public static final String X_API_KEY = "X-Api-Key";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String URL_PREFIX = "https://api.newrelic.com/v2/alerts_nrql_conditions";

    private static final String NR_LAMBDA_RESOURCE_PROVIDER = "NRLambdaResourceProvider";

    private String url;
    private HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public AlertApiClient(String url, HttpClient httpClient) {
        this.url = url;
        this.httpClient = httpClient;
    }

    public JSONObject create(
            NewRelicNrqlCondition newRelicNrqlCondition, String apiKey, int policyId)
            throws IOException, AlertApiException {
        HttpPost alertPost = new HttpPost(String.format("%s/policies/%d.json", this.url, policyId));
        alertPost.addHeader(X_API_KEY, apiKey);
        alertPost.addHeader(USER_AGENT, NR_LAMBDA_RESOURCE_PROVIDER);
        alertPost.addHeader(CONTENT_TYPE, "application/json");
        alertPost.setEntity(
                new StringEntity(
                        mapper.writeValueAsString(
                                Collections.singletonMap(
                                        "nrql_condition", newRelicNrqlCondition))));

        HttpResponse response = this.httpClient.execute(alertPost);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode >= 400) {
            JsonNode jsonNode = mapper.readTree(response.getEntity().getContent());
            String message = jsonNode.findValue("title").asText();
            // Consume entity to avoid leaking the connection
            EntityUtils.consume(response.getEntity());
            throw new AlertApiException(
                    String.format(
                            "Failed to create alert condition for policy ID: %d. Error message: '%s'",
                            policyId, message));
        } else {
            String jsonResponse =
                    EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            JSONObject alertResponse = new JSONObject(jsonResponse);
            return alertResponse.getJSONObject("nrql_condition");
        }
    }

    public JSONObject update(
            NewRelicNrqlCondition newRelicNrqlCondition, String apiKey, int conditionId)
            throws IOException, AlertApiException {
        HttpPut alertPut = new HttpPut(String.format("%s/%d.json", this.url, conditionId));
        alertPut.addHeader(X_API_KEY, apiKey);
        alertPut.addHeader(USER_AGENT, "NRLambdaResourceProvider");
        alertPut.addHeader(CONTENT_TYPE, "application/json");
        alertPut.setEntity(
                new StringEntity(
                        mapper.writeValueAsString(
                                Collections.singletonMap(
                                        "nrql_condition", newRelicNrqlCondition))));

        HttpResponse response = this.httpClient.execute(alertPut);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode >= 400) {
            JsonNode jsonNode = mapper.readTree(response.getEntity().getContent());
            String message = jsonNode.findValue("title").asText();
            // Consume entity to avoid leaking the connection
            EntityUtils.consume(response.getEntity());
            throw new AlertApiException(
                    String.format(
                            "Failed to update alert condition for condition ID: %d. Error message: '%s'",
                            conditionId, message));
        } else {
            String jsonResponse =
                    EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            JSONObject alertResponse = new JSONObject(jsonResponse);
            return alertResponse.getJSONObject("nrql_condition");
        }
    }

    public void remove(String apiKey, int conditionId) throws AlertApiException, IOException {
        HttpDelete delete = new HttpDelete(String.format("%s/%d.json", url, conditionId));
        delete.addHeader(X_API_KEY, apiKey);
        delete.addHeader(USER_AGENT, NR_LAMBDA_RESOURCE_PROVIDER);

        HttpResponse response = this.httpClient.execute(delete);
        int statusCode = response.getStatusLine().getStatusCode();
        // 404 isn't really a failure; the entity doesn't exist, so the post condition is true
        if (statusCode >= 400 && statusCode != 404) {
            EntityUtils.consume(response.getEntity());

            if (statusCode == 401) {
                throw new AlertApiException(
                        String.format(
                                "Unauthorized. Failed to delete alert condition with ID: %d. Probably an invalid API key.",
                                conditionId));
            } else {
                throw new AlertApiException(
                        String.format("Failed to delete alert condition with ID: %d", conditionId));
            }
        }
    }

    public List<NewRelicNrqlCondition> list(String apiKey, int policyId, @Nullable Integer page)
            throws IOException, AlertApiException {
        String requestUrlString = String.format("%s?policy_id=%d", url, policyId);
        if (page != null) {
            requestUrlString += "&page=" + page;
        }
        HttpGet get = new HttpGet(requestUrlString);
        get.addHeader(X_API_KEY, apiKey);
        get.addHeader(USER_AGENT, NR_LAMBDA_RESOURCE_PROVIDER);

        HttpResponse httpResponse = httpClient.execute(get);
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        if (statusCode >= 400) {
            EntityUtils.consume(httpResponse.getEntity());

            if (statusCode == 401) {
                throw new AlertApiException(
                        String.format(
                                "Unauthorized. Failed to list conditions for policy %d. Probably an invalid API key.",
                                policyId));
            } else {
                throw new AlertApiException(
                        String.format("Failed to list alert conditions for policy: %d", policyId));
            }
        }

        Map<String, List<NewRelicNrqlCondition>> response =
                mapper.readValue(
                        httpResponse.getEntity().getContent(),
                        new TypeReference<Map<String, List<NewRelicNrqlCondition>>>() {});
        return response.get("nrql_conditions");
    }

    public Iterator<NewRelicNrqlCondition> listAll(String apiKey, int policyId) {
        return new Iterator<NewRelicNrqlCondition>() {
            private Iterator<NewRelicNrqlCondition> currentPage;
            private int pageNumber = 1;

            @Override
            public boolean hasNext() {
                try {
                    if (currentPage == null || !currentPage.hasNext()) {
                        List<NewRelicNrqlCondition> page = list(apiKey, policyId, pageNumber);
                        currentPage = page.iterator();
                        if (page.isEmpty()) {
                            return false;
                        }
                        ++pageNumber;
                    }
                    return currentPage.hasNext();
                } catch (IOException | AlertApiException e) {
                    return false;
                }
            }

            @Override
            public NewRelicNrqlCondition next() {
                return currentPage.next();
            }
        };
    }

    public NewRelicNrqlCondition get(String apiKey, int policyId, int conditionId)
            throws AlertApiException {
        // There's no get-by-id API, so fake it with list.
        for (Iterator<NewRelicNrqlCondition> it = listAll(apiKey, policyId); it.hasNext(); ) {
            NewRelicNrqlCondition condition = it.next();
            if (condition.getId().equals(conditionId)) {
                return condition;
            }
        }
        throw new AlertApiException(
                String.format("Could not find condition %d in policy %d", conditionId, policyId));
    }
}
