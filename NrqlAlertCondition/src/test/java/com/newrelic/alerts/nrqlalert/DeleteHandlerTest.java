package com.newrelic.alerts.nrqlalert;

import software.amazon.cloudformation.proxy.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @Mock
    private AlertApiClient client;

    private ResourceModel model;

    @BeforeEach
    public void setup() throws IOException {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        client = mock(AlertApiClient.class);
        ObjectMapper mapper = new ObjectMapper();
        Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("nrql-alert-delete-test.json"), StandardCharsets.UTF_8);
        model = mapper.readValue(reader, ResourceModel.class);
    }

    @Test
    public void handleRequest_SimpleDeletion() throws IOException, AlertApiException {
        final DeleteHandler handler = new DeleteHandler(client);

        int conditionId = this.model.getNrqlCondition().getId();
        String conditionType = this.model.getNrqlCondition().getType();
        assertThat(conditionId).isEqualTo(1);
        assertThat(conditionType).isEqualTo("static");

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        // verify the request looks right
        verify(client).remove(eq(this.model.getApiKey()), eq(conditionId));

        // check basic response codes
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_FailedDeletion() throws IOException, AlertApiException {
        final DeleteHandler handler = new DeleteHandler(client);
        Mockito.doThrow(new AlertApiException("test")).when(client).remove(anyString(), anyInt());

        int conditionId = this.model.getNrqlCondition().getId();
        String conditionType = this.model.getNrqlCondition().getType();
        assertThat(conditionId).isEqualTo(1);
        assertThat(conditionType).isEqualTo("static");

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        // check basic response data
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
    }

}
