package com.newrelic.alerts.nrqlalert.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.newrelic.alerts.nrqlalert.Term;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class NewRelicTerm {
    private Integer duration;
    private String operator;
    private String priority;
    private Double threshold;
    private String timeFunction;

    public NewRelicTerm(Term modelTerm) {
        this.setDuration(modelTerm.getDuration());
        this.setOperator(modelTerm.getOperator());
        this.setPriority(modelTerm.getPriority());
        this.setThreshold(modelTerm.getThreshold());
        this.setTimeFunction(modelTerm.getTimeFunction());
    }
}
