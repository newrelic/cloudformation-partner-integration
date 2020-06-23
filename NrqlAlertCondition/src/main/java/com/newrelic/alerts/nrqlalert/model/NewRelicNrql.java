package com.newrelic.alerts.nrqlalert.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.newrelic.alerts.nrqlalert.Nrql;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class NewRelicNrql {
    private String query;
    private Integer sinceValue;

    public NewRelicNrql(Nrql modelNrql) {
        this.setQuery(modelNrql.getQuery());
        this.setSinceValue(modelNrql.getSinceValue());
    }
}
