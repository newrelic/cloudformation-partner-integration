package com.newrelic.alerts.nrqlalert;

import software.amazon.cloudformation.proxy.*;
import com.newrelic.alerts.nrqlalert.model.NewRelicNrql;
import com.newrelic.alerts.nrqlalert.model.NewRelicNrqlCondition;
import com.newrelic.alerts.nrqlalert.model.NewRelicTerm;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReadHandlerTest {
    private static final String TEST_API_KEY = "testApiKey";
    private static final int TEST_CONDITION_ID = 1234;
    private static final int TEST_POLICY_ID = 6789;

    private ReadHandler readHandler;

    private AlertApiClient mockApiClient;
    private AmazonWebServicesClientProxy proxy;
    private CallbackContext context;
    private Logger logger;

    @BeforeEach
    public void setup() {
        mockApiClient = mock(AlertApiClient.class);
        proxy = mock(AmazonWebServicesClientProxy.class);

        context = mock(CallbackContext.class);
        logger = mock(Logger.class);

        readHandler = new ReadHandler(mockApiClient);
    }

    @Test
    public void testHandler() throws AlertApiException {
        ResourceHandlerRequest<ResourceModel> request = new ResourceHandlerRequest<>();

        NrqlCondition nrqlCondition = new NrqlCondition();
        nrqlCondition.setId(TEST_CONDITION_ID);
        request.setDesiredResourceState(new ResourceModel(TEST_API_KEY, TEST_POLICY_ID, nrqlCondition));

        NewRelicNrqlCondition newRelicNrqlCondition = new NewRelicNrqlCondition(
                "test condition",
                TEST_CONDITION_ID,
                "testType",
                "http://runbook.example.com",
                true,
                0,
                true,
                "aValueFunction",
                Lists.newArrayList(new NewRelicTerm("duration", "op", "priority", "threshold", "timeFn")),
                new NewRelicNrql("SELECT something", "some time ago")
        );
        when(mockApiClient.get(anyString(), anyInt(), anyInt())).thenReturn(newRelicNrqlCondition);

        ProgressEvent<ResourceModel, CallbackContext> result = readHandler.handleRequest(proxy, request, context, logger);

        assertEquals(OperationStatus.SUCCESS, result.getStatus());
        assertEquals(newRelicNrqlCondition, new NewRelicNrqlCondition(result.getResourceModel().getNrqlCondition()));
    }
}
