package com.newrelic.alerts.nrqlalert;

import software.amazon.cloudformation.proxy.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;

public class DeleteHandler extends BaseHandler<CallbackContext> {

    private final AlertApiClient alertApiClient;

    public DeleteHandler() {
        alertApiClient = new AlertApiClient(AlertApiClient.URL_PREFIX, HttpClients.createDefault());
    }

    // this will allow us to mock the client and not make real actual calls when testing
    public DeleteHandler(AlertApiClient alertApiClient) {
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
        final int conditionId = model.getNrqlCondition().getId();

        // Try to actually hit the endpoint!
        OperationStatus status = OperationStatus.SUCCESS;
        try {
            logger.log(String.format("Attempting to delete alert condition with ID: %d", conditionId));
            alertApiClient.remove(apiKey, conditionId);
            logger.log(String.format("Deleted alert condition with ID: %d", conditionId));
        } catch (IOException e) {
            status = OperationStatus.FAILED;
            logger.log(ExceptionUtils.getStackTrace(e));
        } catch (AlertApiException e) {
            status = OperationStatus.FAILED;
            logger.log(e.getMessage());
        }

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .resourceModel(model)
            .status(status)
            .build();
    }
}
