package org.graylog.plugins.enterprise.search.engine;

import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.QueryJob;
import org.graylog.plugins.enterprise.search.QueryResult;

public interface QueryBackend {

    /**
     * Generate a backend-specific query out of the logical query structure.
     * @param query the graylog query structure
     * @return a backend specific generated query
     */
    Object generate(Query query);

    /**
     * Run the generated query as part of the given query job.
     *
     * This method is typically being run in an executor and can safely block.
     * @param job currently executing job
     * @param generatedQuery the generated query by {@link #generate(Query)}
     * @return the result for the query
     * @throws RuntimeException if the query could not be executed for some reason
     */
    QueryResult run(QueryJob job, Object generatedQuery);
}
