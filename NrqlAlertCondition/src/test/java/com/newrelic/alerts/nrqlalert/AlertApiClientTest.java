package com.newrelic.alerts.nrqlalert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.alerts.nrqlalert.model.NewRelicNrql;
import com.newrelic.alerts.nrqlalert.model.NewRelicNrqlCondition;
import com.newrelic.alerts.nrqlalert.model.NewRelicTerm;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.util.EntityUtils;
import org.assertj.core.util.Lists;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AlertApiClientTest {
    private static final String TEST_API_KEY = "testApiKey";
    private static final int TEST_CONDITION_ID = 1234;
    private static final int TEST_POLICY_ID = 6789;

    private AlertApiClient alertApiClient;
    private HttpClient mockHttpClient;
    private NewRelicNrqlCondition nrqlCondition;

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(HttpClient.class);
        alertApiClient = new AlertApiClient("http://example.com", mockHttpClient);

        //TODO: make this more realistic, to the extent that it's relevant for these tests.
        List<NewRelicTerm> terms = Lists.newArrayList(new NewRelicTerm());
        nrqlCondition = new NewRelicNrqlCondition(
                "test",
                null,
                "a_condition",
                "http://runbook.example.com",
                true,
                0,
                true,
                "valueFunction",
                terms,
                new NewRelicNrql()
        );
    }

    @Test
    void create() throws IOException, AlertApiException {
        BasicHttpResponse okResponse = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK"));
        okResponse.setEntity(new StringEntity("{\"nrql_condition\": {\"isTest\":true}}", ContentType.APPLICATION_JSON));
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(
                okResponse);

        JSONObject createdCondition = alertApiClient.create(nrqlCondition, TEST_API_KEY, TEST_POLICY_ID);

        assertTrue(createdCondition.getBoolean("isTest"));

        ArgumentCaptor<HttpPost> requestCaptor = ArgumentCaptor.forClass(HttpPost.class);
        verify(mockHttpClient).execute(requestCaptor.capture());
        HttpPost request = requestCaptor.getValue();

        assertEquals("http://example.com/policies/6789.json", request.getURI().toString());
        assertEquals("application/json", request.getFirstHeader(AlertApiClient.CONTENT_TYPE).getValue());
        assertEquals(TEST_API_KEY, request.getFirstHeader(AlertApiClient.X_API_KEY).getValue());

        String requestBody = EntityUtils.toString(request.getEntity(), StandardCharsets.UTF_8);

        //TODO: this is kinda weird. Might be better to assert against a string literal.
        ObjectMapper objectMapper = new ObjectMapper();
        String expectedRequestBody = objectMapper.writeValueAsString(Collections.singletonMap("nrql_condition", nrqlCondition));
        assertEquals(expectedRequestBody, requestBody);
    }

    @Test
    void update() throws IOException, AlertApiException {
        BasicHttpResponse okResponse = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK"));
        okResponse.setEntity(new StringEntity("{\"nrql_condition\": {\"isTest\":true}}", ContentType.APPLICATION_JSON));
        when(mockHttpClient.execute(any(HttpPut.class))).thenReturn(
                okResponse);

        JSONObject createdCondition = alertApiClient.update(nrqlCondition, TEST_API_KEY, TEST_CONDITION_ID);

        assertTrue(createdCondition.getBoolean("isTest"));

        ArgumentCaptor<HttpPut> requestCaptor = ArgumentCaptor.forClass(HttpPut.class);
        verify(mockHttpClient).execute(requestCaptor.capture());
        HttpPut request = requestCaptor.getValue();

        assertEquals("http://example.com/1234.json", request.getURI().toString());
        assertEquals("application/json", request.getFirstHeader(AlertApiClient.CONTENT_TYPE).getValue());
        assertEquals(TEST_API_KEY, request.getFirstHeader(AlertApiClient.X_API_KEY).getValue());

        String requestBody = EntityUtils.toString(request.getEntity(), StandardCharsets.UTF_8);

        //TODO: this is kinda weird. Might be better to assert against a string literal.
        ObjectMapper objectMapper = new ObjectMapper();
        String expectedRequestBody = objectMapper.writeValueAsString(Collections.singletonMap("nrql_condition", nrqlCondition));
        assertEquals(expectedRequestBody, requestBody);
    }

    @Test
    void remove() throws IOException, AlertApiException {
        when(mockHttpClient.execute(any(HttpDelete.class)))
                .thenReturn(new BasicHttpResponse(
                        new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK")));

        alertApiClient.remove(TEST_API_KEY, TEST_CONDITION_ID);

        ArgumentCaptor<HttpDelete> requestCaptor = ArgumentCaptor.forClass(HttpDelete.class);
        verify(mockHttpClient).execute(requestCaptor.capture());
        HttpDelete request = requestCaptor.getValue();

        assertEquals("http://example.com/1234.json", request.getURI().toString());
        assertEquals(TEST_API_KEY, request.getFirstHeader(AlertApiClient.X_API_KEY).getValue());
    }

    @Test
    void removeUnauth() throws IOException {
        when(mockHttpClient.execute(any(HttpDelete.class)))
                .thenReturn(new BasicHttpResponse(
                        new BasicStatusLine(HttpVersion.HTTP_1_1, 401, "Unauthorized")));

        try {
            alertApiClient.remove(TEST_API_KEY, TEST_CONDITION_ID);
            fail("Expected AlertApiException");
        } catch (AlertApiException e) {
            assertEquals("Unauthorized. Failed to delete alert condition with ID: 1234. Probably an invalid API key.", e.getMessage());
        }
    }

    @Test
    void removeForbidden() throws IOException {
        when(mockHttpClient.execute(any(HttpDelete.class)))
                .thenReturn(new BasicHttpResponse(
                        new BasicStatusLine(HttpVersion.HTTP_1_1, 403, "Unauthorized")));

        try {
            alertApiClient.remove(TEST_API_KEY, TEST_CONDITION_ID);
            fail("Expected AlertApiException");
        } catch (AlertApiException e) {
            assertEquals("Failed to delete alert condition with ID: 1234", e.getMessage());
        }
    }
}