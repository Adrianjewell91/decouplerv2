package com.decoupler.impl.decomposition;

import java.util.Map;

/**
 * A single step within a {@link DecompositionPlan}.
 *
 * <p>Jackson POJO: requires a no-arg constructor plus getters/setters.
 *
 * <p>Expression syntax in {@code filter} and {@code values} maps:
 * <ul>
 *   <li>{@code "$0"}, {@code "$1"}, … — positional transaction parameters</li>
 *   <li>{@code "$stepId.fieldName"} — column from an ancestor step's result row</li>
 *   <li>any other value — treated as a literal string</li>
 * </ul>
 */
public class PlanStep {

    private String id;
    private PlanStepType type;
    private String fragment;
    private Map<String, String> filter;
    private Map<String, String> values;
    private String parent;

    public PlanStep() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public PlanStepType getType() { return type; }
    public void setType(PlanStepType type) { this.type = type; }

    public String getFragment() { return fragment; }
    public void setFragment(String fragment) { this.fragment = fragment; }

    public Map<String, String> getFilter() { return filter; }
    public void setFilter(Map<String, String> filter) { this.filter = filter; }

    public Map<String, String> getValues() { return values; }
    public void setValues(Map<String, String> values) { this.values = values; }

    public String getParent() { return parent; }
    public void setParent(String parent) { this.parent = parent; }

    @Override
    public String toString() {
        return "PlanStep{id='" + id + "', type=" + type + ", fragment='" + fragment + "', parent='" + parent + "'}";
    }
}
