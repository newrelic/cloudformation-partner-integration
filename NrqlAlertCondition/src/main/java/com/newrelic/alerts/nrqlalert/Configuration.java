package com.newrelic.alerts.nrqlalert;

import org.json.JSONObject;
import org.json.JSONTokener;

class Configuration extends BaseConfiguration {
    public Configuration() {
        super("newrelic-alerts-nrqlalert.json");
    }

    public JSONObject resourceSchemaJSONObject() {
        return new JSONObject(
                new JSONTokener(
                        this.getClass().getClassLoader().getResourceAsStream(schemaFilename)));
    }
}
