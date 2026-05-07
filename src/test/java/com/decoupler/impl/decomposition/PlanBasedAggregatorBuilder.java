package com.decoupler.impl.decomposition;

import com.decoupler.interfaces.Aggregator;
import com.decoupler.interfaces.BuildException;
import com.decoupler.interfaces.LlmClient;
import com.decoupler.interfaces.Partition;
import com.decoupler.interfaces.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link Aggregator.Builder} that calls an {@link LlmClient} to generate a
 * {@link DecompositionPlan} JSON, then parses it and returns a {@link PlanBasedAggregator}.
 */
public class PlanBasedAggregatorBuilder implements Aggregator.Builder {

    private Transaction tx;
    private Partition partition;
    private LlmClient llmClient;
    private Duration timeout;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public Aggregator.Builder transaction(Transaction tx) {
        this.tx = tx;
        return this;
    }

    @Override
    public Aggregator.Builder partition(Partition partition) {
        this.partition = partition;
        return this;
    }

    @Override
    public Aggregator.Builder llmClient(LlmClient llmClient) {
        this.llmClient = llmClient;
        return this;
    }

    @Override
    public Aggregator.Builder timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    @Override
    public Aggregator build() throws BuildException {
        if (tx == null) throw new BuildException("transaction() must be set before build()");
        if (partition == null) throw new BuildException("partition() must be set before build()");
        if (llmClient == null) throw new BuildException("llmClient() must be set before build()");
        if (timeout == null) throw new BuildException("timeout() must be set before build()");

        String prompt = buildPrompt();

        String response;
        try {
            response = llmClient.generate(prompt, timeout);
        } catch (RuntimeException e) {
            throw new BuildException("LLM call failed: " + e.getMessage(), e);
        }

        String json = extractJson(response);

        DecompositionPlan plan;
        try {
            plan = MAPPER.readValue(json, DecompositionPlan.class);
        } catch (Exception e) {
            throw new BuildException(
                    "Failed to parse DecompositionPlan JSON for transaction [" + tx.id() + "]. "
                    + "Raw LLM output:\n" + response, e);
        }

        if (plan.getQueryPattern() == null || plan.getQueryPattern().isBlank()) {
            plan.setQueryPattern(tx.id());
        }

        List<String> availableFragments = partition.fragments().stream()
                .map(f -> f.schema().name())
                .collect(Collectors.toList());

        if (plan.getSteps() != null) {
            for (PlanStep step : plan.getSteps()) {
                String fragName = step.getFragment();
                if (fragName != null && !availableFragments.contains(fragName)) {
                    throw new BuildException(
                            "Plan step [" + step.getId() + "] references fragment '" + fragName
                            + "' but only available: " + availableFragments);
                }
            }
        }

        return new PlanBasedAggregator(plan, new PlanInterpreter());
    }

    // -------------------------------------------------------------------------
    // Prompt construction
    // -------------------------------------------------------------------------

    private String buildPrompt() {
        String fragmentsDesc = partition.fragments().stream()
                .map(f -> {
                    String fieldList = f.schema().fields().stream()
                            .map(fd -> fd.name() + " " + fd.typeHint())
                            .collect(Collectors.joining(", "));
                    return "  " + f.schema().name() + "(" + fieldList + ")";
                })
                .collect(Collectors.joining("\n"));

        String commandsDesc = partition.commands().stream()
                .map(c -> "  " + c.describe())
                .collect(Collectors.joining("\n"));

        return """
                You are a SQL decomposition engine. Convert the given SQL transaction into a \
                DecompositionPlan JSON that replaces it with fragment operations.

                STRICT RULES:
                1. Return ONLY valid JSON — no markdown fences, no explanation, no extra text.
                2. Do NOT wrap the JSON in ```json or ```.
                3. The JSON must exactly match one of these structures:

                For READ transactions:
                {
                  "queryPattern": "<transaction id>",
                  "type": "READ",
                  "steps": [
                    { "id": "s1", "type": "READ", "fragment": "<table>", "filter": {"<col>": "$0"} },
                    { "id": "s2", "type": "READ", "fragment": "<table>", "filter": {"<col>": "$s1.<col>"}, "parent": "s1" }
                  ],
                  "leafStep": "s2",
                  "resultColumns": ["s1.<col>", "s2.<col>:<alias>"]
                }

                For WRITE transactions:
                {
                  "queryPattern": "<transaction id>",
                  "type": "WRITE",
                  "steps": [
                    { "id": "s1", "type": "INSERT", "fragment": "<table>", "values": {"<col>": "$0", "<col>": "$1"} }
                  ]
                }

                PARAMETER REFERENCES in filter/values maps:
                - "$0", "$1", "$2" etc. → positional transaction parameters (0-indexed)
                - "$stepId.<column>" → column value from a previous step's result row

                STEP TYPES: READ (for selects), INSERT, UPDATE, DELETE

                AVAILABLE FRAGMENTS (schema):
                """ + fragmentsDesc + """

                AVAILABLE COMMANDS:
                """ + commandsDesc + """

                TRANSACTION ID: """ + tx.id() + """

                TRANSACTION LOGIC (SQL):
                """ + tx.logic() + """

                PARAMETERS: """ + tx.params() + """

                Return only the JSON:
                """;
    }

    // -------------------------------------------------------------------------
    // JSON extraction
    // -------------------------------------------------------------------------

    private String extractJson(String raw) {
        String cleaned = raw.replaceAll("(?s)<think>.*?</think>", "").trim();
        cleaned = cleaned.replaceAll("(?s)```(?:json)?\\s*", "").replaceAll("```", "").trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }
}
