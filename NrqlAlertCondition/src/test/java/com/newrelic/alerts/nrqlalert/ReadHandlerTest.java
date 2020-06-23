package com.newrelic.alerts.nrqlalert;

import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.alerts.nrqlalert.model.NewRelicNrql;
import com.newrelic.alerts.nrqlalert.model.NewRelicNrqlCondition;
import com.newrelic.alerts.nrqlalert.model.NewRelicTerm;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.cloudformation.proxy.*;

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
        request.setDesiredResourceState(
                new ResourceModel(TEST_API_KEY, TEST_POLICY_ID, nrqlCondition));

        NewRelicNrqlCondition newRelicNrqlCondition =
                new NewRelicNrqlCondition(
                        true,
                        1,
                        TEST_CONDITION_ID,
                        false,
                        "test condition",
                        "http://runbook.example.com",
                        "testType",
                        "average",
                        3600,
                        Lists.newArrayList(new NewRelicTerm(1, "above", "critical", 20d, "all")),
                        new NewRelicNrql("SELECT something", 3));
        when(mockApiClient.get(anyString(), anyInt(), anyInt())).thenReturn(newRelicNrqlCondition);

        ProgressEvent<ResourceModel, CallbackContext> result =
                readHandler.handleRequest(proxy, request, context, logger);

        assertEquals(OperationStatus.SUCCESS, result.getStatus());
        assertEquals(
                newRelicNrqlCondition,
                new NewRelicNrqlCondition(result.getResourceModel().getNrqlCondition()));
    }
}
