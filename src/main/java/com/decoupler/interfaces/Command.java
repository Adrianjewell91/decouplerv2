package com.decoupler.interfaces;

import java.util.Map;

/**
 * A self-contained operation that can be executed against a {@link Fragment}.
 *
 * <p>Mirrors {@link Transaction}: {@code build(Schema)} produces the SQL string,
 * {@code params()} provides the bind values. The Fragment executes both against
 * its own DataSource — Command has no reference to Fragment.
 */
public interface Command {
    String build(Schema schema);
    Map<String, Object> params();
    String describe();
}
