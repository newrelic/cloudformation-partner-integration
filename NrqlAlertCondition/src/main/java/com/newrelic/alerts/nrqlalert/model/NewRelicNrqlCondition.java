package com.newrelic.alerts.nrqlalert.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.newrelic.alerts.nrqlalert.Nrql;
import com.newrelic.alerts.nrqlalert.NrqlCondition;
import com.newrelic.alerts.nrqlalert.Term;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class NewRelicNrqlCondition {
    private Boolean enabled;
    private Integer expectedGroups;
    private Integer id;
    private Boolean ignoreOverlap;
    private String name;
    private String runbookUrl;
    private String type;
    private String valueFunction;
    private Integer violationTimeLimitSeconds;

    private List<NewRelicTerm> terms;
    private NewRelicNrql nrql;

    public NewRelicNrqlCondition(NrqlCondition modelNrqlCondition) {
        this.setEnabled(modelNrqlCondition.getEnabled());
        this.setExpectedGroups(modelNrqlCondition.getExpectedGroups());
        this.setId(modelNrqlCondition.getId());
        this.setIgnoreOverlap(modelNrqlCondition.getIgnoreOverlap());
        this.setName(modelNrqlCondition.getName());
        this.setRunbookUrl(modelNrqlCondition.getRunbookUrl());
        this.setType(modelNrqlCondition.getType());
        this.setValueFunction(modelNrqlCondition.getValueFunction());
        this.setViolationTimeLimitSeconds(modelNrqlCondition.getViolationTimeLimitSeconds());

        this.setTerms(
                modelNrqlCondition.getTerms().stream()
                        .map(NewRelicTerm::new)
                        .collect(Collectors.toList()));
        this.setNrql(new NewRelicNrql(modelNrqlCondition.getNrql()));
    }

    public void updateNrqlCondition(NrqlCondition other) {
        other.setEnabled(this.getEnabled());
        other.setExpectedGroups(this.getExpectedGroups());
        other.setIgnoreOverlap(this.getIgnoreOverlap());
        other.setName(this.getName());
        other.setRunbookUrl(this.getRunbookUrl());
        other.setType(this.getType());
        other.setValueFunction(this.getValueFunction());
        other.setViolationTimeLimitSeconds(this.getViolationTimeLimitSeconds());

        Nrql nrql = new Nrql(this.getNrql().getQuery(), this.getNrql().getSinceValue());
        other.setNrql(nrql);

        // update all terms
        List<Term> terms =
                this.getTerms().stream()
                        .map(
                                t -> {
                                    Term term = new Term();
                                    term.setDuration(t.getDuration());
                                    term.setOperator(t.getOperator());
                                    term.setPriority(t.getPriority());
                                    term.setThreshold(t.getThreshold());
                                    term.setTimeFunction(t.getTimeFunction());
                                    return term;
                                })
                        .collect(Collectors.toList());
        other.setTerms(terms);
    }
}
