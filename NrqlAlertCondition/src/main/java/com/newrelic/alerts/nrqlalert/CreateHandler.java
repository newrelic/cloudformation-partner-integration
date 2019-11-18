package com.newrelic.alerts.nrqlalert;

import software.amazon.cloudformation.proxy.*;
import com.newrelic.alerts.nrqlalert.model.NewRelicNrqlCondition;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;

import java.io.IOException;

public class CreateHandler extends BaseHandler<CallbackContext> {


    private AlertApiClient alertApiClient;

    public CreateHandler() {
        this.alertApiClient = new AlertApiClient(AlertApiClient.URL_PREFIX, HttpClients.createDefault());
    }

    // this will allow us to mock the client and not make real actual calls when testing
    public CreateHandler(AlertApiClient alertApiClient) {
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
        final int policyId = model.getPolicyId();
        OperationStatus status = OperationStatus.SUCCESS;
        NrqlCondition modelNrqlCondition = model.getNrqlCondition();
        // convert to our New Relic-specific class
        NewRelicNrqlCondition newRelicNrqlCondition = new NewRelicNrqlCondition(modelNrqlCondition);


        try {
            logger.log(String.format("Attempting to create alert condition for policy ID: %d", policyId));
            JSONObject nrqlConditionJson = alertApiClient.create(newRelicNrqlCondition, apiKey, policyId);
            int id = nrqlConditionJson.getInt("id");
            model.getNrqlCondition().setId(id);
            model.getNrqlCondition().setType(nrqlConditionJson.getString("type"));
            logger.log(String.format("Created alert condition with ID %d in policy %d", id, policyId));

        } catch (IOException | AlertApiException e) {
            status = OperationStatus.FAILED;
            logger.log(ExceptionUtils.getStackTrace(e));
        }

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .resourceModel(model)
            .status(status)
            .build();
    }
}
