package com.newrelic.alerts.nrqlalert;

import com.newrelic.alerts.nrqlalert.model.NewRelicNrqlCondition;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class UpdateHandler extends BaseHandler<CallbackContext> {
    private AlertApiClient alertApiClient;

    public UpdateHandler() {
        this.alertApiClient =
                new AlertApiClient(AlertApiClient.URL_PREFIX, HttpClients.createDefault());
    }

    // this will allow us to mock the client and not make real actual calls when testing
    public UpdateHandler(AlertApiClient alertApiClient) {
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
        OperationStatus status = OperationStatus.SUCCESS;
        NrqlCondition modelNrqlCondition = model.getNrqlCondition();
        // convert to our New Relic-specific class
        NewRelicNrqlCondition newRelicNrqlCondition = new NewRelicNrqlCondition(modelNrqlCondition);

        try {
            logger.log(
                    String.format(
                            "Attempting to update alert condition for condition ID: %d",
                            conditionId));
            JSONObject nrqlConditionJson =
                    alertApiClient.update(newRelicNrqlCondition, apiKey, conditionId);
            NrqlCondition nrqlCondition = model.getNrqlCondition();
            nrqlCondition.setEnabled(nrqlConditionJson.getBoolean("enabled"));
            nrqlCondition.setExpectedGroups(nrqlConditionJson.getInt("expected_groups"));
            nrqlCondition.setIgnoreOverlap(nrqlConditionJson.getBoolean("ignore_overlap"));
            nrqlCondition.setName(nrqlConditionJson.getString("name"));
            nrqlCondition.setRunbookUrl(nrqlConditionJson.getString("runbook_url"));
            nrqlCondition.setType(nrqlConditionJson.getString("type"));
            nrqlCondition.setValueFunction(nrqlConditionJson.getString("value_function"));
            nrqlCondition.setViolationTimeLimitSeconds(
                    nrqlConditionJson.getInt("violation_time_limit_seconds"));

            // update query info
            Nrql nrql = new Nrql();
            JSONObject nrqlJson = nrqlConditionJson.getJSONObject("nrql");
            nrql.setQuery(nrqlJson.getString("query"));
            nrql.setSinceValue(nrqlJson.getInt("since_value"));
            nrqlCondition.setNrql(nrql);

            // update all terms
            ArrayList<Term> terms = new ArrayList<>();
            JSONArray termsJson = nrqlConditionJson.getJSONArray("terms");
            termsJson.forEach(
                    termObj -> {
                        JSONObject termJson = (JSONObject) termObj;
                        Term term = new Term();
                        term.setDuration(termJson.getInt("duration"));
                        term.setOperator(termJson.getString("operator"));
                        term.setPriority(termJson.getString("priority"));
                        term.setThreshold((double) termJson.getFloat("threshold"));
                        term.setTimeFunction(termJson.getString("time_function"));
                        terms.add(term);
                    });
            nrqlCondition.setTerms(terms);

            logger.log(String.format("Updated alert condition with ID: %d", conditionId));

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
