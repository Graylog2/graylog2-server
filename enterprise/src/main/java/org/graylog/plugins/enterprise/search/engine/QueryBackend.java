package org.graylog.plugins.enterprise.search.engine;

import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.QueryInfo;
import org.graylog.plugins.enterprise.search.QueryResult;
import org.graylog.plugins.enterprise.search.SearchJob;

import java.util.Set;

public interface QueryBackend {

    /**
     * Generate a backend-specific query out of the logical query structure.
     * @param job currently executing job
     * @param query the graylog query structure
     * @param predecessorResults the query result of the preceding queries
     * @return a backend specific generated query
     */
    Object generate(SearchJob job, Query query, Set<QueryResult> predecessorResults);

    /**
     * Run the generated query as part of the given query job.
     *
     * This method is typically being run in an executor and can safely block.
     * @param job currently executing job
     * @param query the individual query to run from the current job
     * @param generatedQuery the generated query by {@link #generate(SearchJob, Query, Set)}
     * @param predecessorResults the query result of the preceding queries
     * @return the result for the query
     * @throws RuntimeException if the query could not be executed for some reason
     */
    QueryResult run(SearchJob job, Query query, Object generatedQuery, Set<QueryResult> predecessorResults);

    /**
     * Parse the query and return structural information about it.
     *
     * This method decomposes the backend-specific query and returns information about used parameters, optionally the
     * AST for syntax highlight and other information the UI can use to offer help.
     */
    QueryInfo parse(Query query);
}
