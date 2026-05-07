package com.decoupler.impl.decomposition;

import com.decoupler.impl.sql.SqlDeleteCommand;
import com.decoupler.impl.sql.SqlInsertCommand;
import com.decoupler.impl.sql.SqlReadCommand;
import com.decoupler.impl.sql.SqlResult;
import com.decoupler.impl.sql.SqlUpdateCommand;
import com.decoupler.interfaces.Command;
import com.decoupler.interfaces.Fragment;
import com.decoupler.interfaces.Partition;
import com.decoupler.interfaces.Result;
import com.decoupler.interfaces.Transaction;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Executes a {@link DecompositionPlan} against a {@link Partition}.
 *
 * <p>Internal collaborator used only by {@link PlanBasedAggregator}; has no interface.
 *
 * <h2>READ execution</h2>
 * Performs a recursive tree walk: the root step runs first; child steps run once
 * per parent row with filter values resolved from the parent row. Leaf rows are
 * collected, optionally projected, and returned.
 *
 * <h2>WRITE execution</h2>
 * Steps run in declaration order. Affected-row counts are accumulated.
 *
 * <h2>Expression syntax</h2>
 * <pre>
 *   "$0", "$1", …         → tx.params().get("0"), …
 *   "$stepId.field"        → column from an ancestor step's result row
 *   anything else          → treated as a literal string
 * </pre>
 */
public class PlanInterpreter {

    Result execute(Transaction tx, DecompositionPlan plan, Partition partition) {
        if ("READ".equalsIgnoreCase(plan.getType())) {
            return executeRead(tx, plan, partition);
        } else {
            return executeWrite(tx, plan, partition);
        }
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    private Result executeRead(Transaction tx, DecompositionPlan plan, Partition partition) {
        List<PlanStep> rootSteps = plan.getSteps().stream()
                .filter(s -> s.getParent() == null || s.getParent().isBlank())
                .collect(Collectors.toList());

        List<ExecutionNode> leaves = new ArrayList<>();
        for (PlanStep root : rootSteps) {
            executeStep(root, null, plan, tx, partition, leaves);
        }

        return projectLeaves(leaves, plan.getResultColumns());
    }

    private void executeStep(PlanStep step,
                              ExecutionNode parentNode,
                              DecompositionPlan plan,
                              Transaction tx,
                              Partition partition,
                              List<ExecutionNode> leaves) {
        Map<String, Object> resolvedFilter = resolveExpressions(
                step.getFilter() != null ? step.getFilter() : Map.of(), tx, parentNode);

        Fragment fragment = findFragment(step.getFragment(), partition);
        Result readResult = fragment.execute(new SqlReadCommand(resolvedFilter));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) readResult.message();

        List<PlanStep> children = plan.getSteps().stream()
                .filter(s -> step.getId().equals(s.getParent()))
                .collect(Collectors.toList());

        for (Map<String, Object> row : rows) {
            ExecutionNode node = new ExecutionNode(step.getId(), row, parentNode);
            if (children.isEmpty()) {
                leaves.add(node);
            } else {
                for (PlanStep child : children) {
                    executeStep(child, node, plan, tx, partition, leaves);
                }
            }
        }
    }

    private Result projectLeaves(List<ExecutionNode> leaves, List<String> resultColumns) {
        if (resultColumns == null || resultColumns.isEmpty()) {
            List<Map<String, Object>> rows = leaves.stream()
                    .map(ExecutionNode::row)
                    .collect(Collectors.toList());
            return SqlResult.ofRows(rows);
        }

        List<Map<String, Object>> projected = new ArrayList<>();
        for (ExecutionNode leaf : leaves) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (String colSpec : resultColumns) {
                String[] parts = colSpec.split(":", 2);
                String projection = parts[0].trim();
                int dot = projection.indexOf('.');
                if (dot < 0) continue;
                String stepId = projection.substring(0, dot);
                String colName = projection.substring(dot + 1);
                String outputKey = parts.length > 1 ? parts[1].trim() : colName;
                Object value = findInAncestry(leaf, stepId, colName);
                row.put(outputKey, value);
            }
            projected.add(row);
        }
        return SqlResult.ofRows(projected);
    }

    // -------------------------------------------------------------------------
    // WRITE
    // -------------------------------------------------------------------------

    private Result executeWrite(Transaction tx, DecompositionPlan plan, Partition partition) {
        int totalAffected = 0;

        for (PlanStep step : plan.getSteps()) {
            Fragment fragment = findFragment(step.getFragment(), partition);

            Map<String, Object> resolvedFilter = resolveExpressions(
                    step.getFilter() != null ? step.getFilter() : Map.of(), tx, null);
            Map<String, Object> resolvedValues = resolveExpressions(
                    step.getValues() != null ? step.getValues() : Map.of(), tx, null);

            Command command = switch (step.getType()) {
                case INSERT -> new SqlInsertCommand(resolvedValues);
                case UPDATE -> new SqlUpdateCommand(resolvedValues, resolvedFilter);
                case DELETE -> new SqlDeleteCommand(resolvedFilter);
                default -> null;
            };
            if (command != null) {
                Result r = fragment.execute(command);
                totalAffected += (int) r.message();
            }
        }
        return SqlResult.ofCount(totalAffected);
    }

    // -------------------------------------------------------------------------
    // Expression resolution
    // -------------------------------------------------------------------------

    private Map<String, Object> resolveExpressions(Map<String, String> raw,
                                                    Transaction tx,
                                                    ExecutionNode currentNode) {
        Map<String, Object> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : raw.entrySet()) {
            resolved.put(e.getKey(), resolveExpr(e.getValue(), tx, currentNode));
        }
        return resolved;
    }

    private Object resolveExpr(String expr, Transaction tx, ExecutionNode currentNode) {
        if (expr == null) return null;

        if (expr.startsWith("$")) {
            String ref = expr.substring(1);
            int dot = ref.indexOf('.');
            if (dot < 0) {
                try {
                    int idx = Integer.parseInt(ref);
                    return tx.params().get(String.valueOf(idx));
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Invalid expression: " + expr, ex);
                }
            } else {
                String stepId = ref.substring(0, dot);
                String col = ref.substring(dot + 1);
                if (currentNode == null) {
                    throw new IllegalStateException(
                            "Expression '" + expr + "' requires a parent row but none is available");
                }
                return findInAncestry(currentNode, stepId, col);
            }
        }

        return expr;
    }

    // -------------------------------------------------------------------------
    // Ancestry traversal
    // -------------------------------------------------------------------------

    private Object findInAncestry(ExecutionNode node, String stepId, String column) {
        ExecutionNode current = node;
        while (current != null) {
            if (current.stepId().equals(stepId)) {
                return current.row().get(column);
            }
            current = current.parent();
        }
        throw new IllegalArgumentException(
                "Step '" + stepId + "' not found in ancestry (looking for column '" + column + "')");
    }

    // -------------------------------------------------------------------------
    // Fragment lookup
    // -------------------------------------------------------------------------

    private Fragment findFragment(String name, Partition partition) {
        return partition.fragments().stream()
                .filter(f -> f.schema().name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No fragment named '" + name + "' in partition. Available: "
                        + partition.fragments().stream()
                                .map(f -> f.schema().name())
                                .collect(Collectors.joining(", "))));
    }

    // -------------------------------------------------------------------------
    // ExecutionNode
    // -------------------------------------------------------------------------

    record ExecutionNode(String stepId, Map<String, Object> row, ExecutionNode parent) {}
}
