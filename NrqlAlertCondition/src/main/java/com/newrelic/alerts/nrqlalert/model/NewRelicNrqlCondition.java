package com.newrelic.alerts.nrqlalert.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.newrelic.alerts.nrqlalert.NrqlCondition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class NewRelicNrqlCondition {

    private String name;

    private Integer id;

    private String type;

    private String runbookUrl;

    private Boolean enabled;

    private Integer expectedGroups;

    private Boolean ignoreOverlap;

    private String valueFunction;

    private List<NewRelicTerm> terms;

    private NewRelicNrql nrql;

    public NewRelicNrqlCondition(NrqlCondition modelNrqlCondition) {
        this.setId(modelNrqlCondition.getId());
        this.setType(modelNrqlCondition.getType());
        this.setEnabled(modelNrqlCondition.getEnabled());
        this.setExpectedGroups(modelNrqlCondition.getExpectedGroups());
        this.setIgnoreOverlap(modelNrqlCondition.getIgnoreOverlap());
        this.setName(modelNrqlCondition.getName());
        this.setRunbookUrl(modelNrqlCondition.getRunbookUrl());
        this.setValueFunction(modelNrqlCondition.getValueFunction());
        this.setTerms(modelNrqlCondition.getTerms().stream().map(NewRelicTerm::new).collect(Collectors.toList()));
        this.setNrql(new NewRelicNrql(modelNrqlCondition.getNrql()));
    }
}
