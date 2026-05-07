package com.decoupler.interfaces;

import java.util.List;

/**
 * Strategy that discovers and returns the fragments of a data source.
 *
 * <p>The {@link Monolith} receives this list, initialises each fragment,
 * attaches commands, and assembles the final {@link Partition}.
 */
public interface Partitioner {
    List<Fragment> fragments();
}
