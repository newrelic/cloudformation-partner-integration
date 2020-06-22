package com.newrelic.alerts.nrqlalert;

import com.newrelic.alerts.nrqlalert.model.NewRelicNrqlCondition;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.impl.client.HttpClients;
import software.amazon.cloudformation.proxy.*;

public class ReadHandler extends BaseHandler<CallbackContext> {
    private AlertApiClient alertApiClient;

    public ReadHandler() {
        this.alertApiClient =
                new AlertApiClient(AlertApiClient.URL_PREFIX, HttpClients.createDefault());
    }

    // this will allow us to mock the client and not make real actual calls when testing
    public ReadHandler(AlertApiClient alertApiClient) {
        this.alertApiClient = alertApiClient;
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final String apiKey = model.getApiKey();
        OperationStatus status = OperationStatus.SUCCESS;

        try {
            final int conditionId = model.getNrqlCondition().getId();
            final int policyId = model.getPolicyId();
            logger.log(
                    String.format(
                            "Attempting to read alert condition for condition %d from policy %d",
                            conditionId, policyId));
            NewRelicNrqlCondition condition = alertApiClient.get(apiKey, policyId, conditionId);

            NrqlCondition nrqlCondition = model.getNrqlCondition();
            condition.updateNrqlCondition(nrqlCondition);

            logger.log(String.format("Read alert condition with ID: %d", conditionId));

        } catch (AlertApiException e) {

            status = OperationStatus.FAILED;
            logger.log(ExceptionUtils.getStackTrace(e));
        }

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(status)
                .build();
    }
}
