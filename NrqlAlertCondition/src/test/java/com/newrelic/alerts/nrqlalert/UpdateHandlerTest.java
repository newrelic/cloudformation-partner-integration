package com.newrelic.alerts.nrqlalert;

import com.amazonaws.cloudformation.proxy.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.alerts.nrqlalert.model.NewRelicNrqlCondition;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @Mock
    private AlertApiClient alertApiClient;

    private ResourceModel model;

    @BeforeEach
    public void setup() throws IOException {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        alertApiClient = mock(AlertApiClient.class);
        ObjectMapper mapper = new ObjectMapper();
        Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("nrql-alert-update-test.json"), StandardCharsets.UTF_8);
        model = mapper.readValue(reader, ResourceModel.class);
    }

    @Test
    public void handleRequest_SimpleSuccess() throws IOException, AlertApiException {
        UpdateHandler updateHandler = new UpdateHandler(alertApiClient);

        // do some before assertions
        assertThat(model.getNrqlCondition().getId()).isEqualTo(1);
        assertThat(model.getNrqlCondition().getName()).isEqualTo("TestNRQLCondition");

        // mock the json return data
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "NrqlAlert");
        jsonObject.put("id", "1");
        jsonObject.put("name", "RenamedTestCondition");
        jsonObject.put("runbook_url", "www.example.com/runbook");
        jsonObject.put("enabled", false);
        jsonObject.put("expected_groups", 1);
        jsonObject.put("ignore_overlap", true);
        jsonObject.put("value_function", "string");
        JSONArray termsArray = new JSONArray();
        JSONObject termObject = new JSONObject();
        termObject.put("duration", "2");
        termObject.put("operator", "equal");
        termObject.put("priority", "critical");
        termObject.put("threshold", "20");
        termObject.put("time_function", "all");
        termsArray.put(termObject);
        jsonObject.put("terms", termsArray);
        JSONObject nrqlObject = new JSONObject();
        nrqlObject.put("query", "SELECT count(*) from SomeInterestingTable");
        nrqlObject.put("since_value", "2");
        jsonObject.put("nrql", nrqlObject);

        when(alertApiClient.update(any(NewRelicNrqlCondition.class), any(String.class), any(Integer.class))).thenReturn(jsonObject);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = updateHandler.handleRequest(proxy, request, null, logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);

        // verify that the model was actually updated with returned data (see json object data above)
        NrqlCondition afterCondition = this.model.getNrqlCondition();
        assertThat(afterCondition.getTerms().isEmpty()).isFalse();
        Term firstTerm = afterCondition.getTerms().get(0);
        assertThat(afterCondition.getId()).isEqualTo(1);
        assertThat(afterCondition.getType()).isEqualTo("NrqlAlert");
        assertThat(afterCondition.getEnabled()).isFalse();
        assertThat(firstTerm.getThreshold()).isEqualTo("20");
        assertThat(firstTerm.getPriority()).isEqualTo("critical");
        assertThat(firstTerm.getOperator()).isEqualTo("equal");
        assertThat(afterCondition.getName()).isEqualTo("RenamedTestCondition");
    }

    @Test
    public void handleRequest_SimpleFailure() throws IOException, AlertApiException {
        UpdateHandler updateHandler = new UpdateHandler(alertApiClient);

        // do some before assertions
        assertThat(model.getNrqlCondition().getId()).isEqualTo(1);
        assertThat(model.getNrqlCondition().getName()).isEqualTo("TestNRQLCondition");

        // oh no, bad request! Throw an exception.
        when(alertApiClient.update(any(NewRelicNrqlCondition.class), any(String.class), any(Integer.class))).thenThrow(AlertApiException.class);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = updateHandler.handleRequest(proxy, request, null, logger);

        // Our final response should be a failure
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);

        // Our model should be unchanged
        assertThat(model.getNrqlCondition().getId()).isEqualTo(1);
        assertThat(model.getNrqlCondition().getName()).isEqualTo("TestNRQLCondition");
    }
}


