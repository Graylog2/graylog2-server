package org.graylog.plugins.enterprise.search.engine;

import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.QueryInfo;
import org.graylog.plugins.enterprise.search.QueryResult;
import org.graylog.plugins.enterprise.search.SearchJob;

import java.util.Set;

/**
 * A search backend that is capable of generating and executing search jobs
 *
 * @param <T> the type of the generated query
 */
public interface QueryBackend<T extends GeneratedQueryContext> {

    /**
     * Generate a backend-specific query out of the logical query structure.
     *
     * @param job                currently executing job
     * @param query              the graylog query structure
     * @param predecessorResults the query result of the preceding queries
     * @return a backend specific generated query
     */
    T generate(SearchJob job, Query query, Set<QueryResult> predecessorResults);

    // TODO we can probably push job, query and predecessorResults into the GeneratedQueryContext to simplify the signature
    default QueryResult run(SearchJob job, Query query, GeneratedQueryContext generatedQueryContext, Set<QueryResult> predecessorResults) {
        // https://www.ibm.com/developerworks/java/library/j-jtp04298/index.html#3.0
        //noinspection unchecked
        return doRun(job, query, (T) generatedQueryContext, predecessorResults);
    }

    /**
     * Run the generated query as part of the given query job.
     * <p>
     * This method is typically being run in an executor and can safely block.
     *
     * @param job                currently executing job
     * @param query              the individual query to run from the current job
     * @param queryContext       the generated query by {@link #generate(SearchJob, Query, Set)}
     * @param predecessorResults the query result of the preceding queries
     * @return the result for the query
     * @throws RuntimeException if the query could not be executed for some reason
     */
    QueryResult doRun(SearchJob job, Query query, T queryContext, Set<QueryResult> predecessorResults);

    /**
     * Parse the query and return structural information about it.
     * <p>
     * This method decomposes the backend-specific query and returns information about used parameters, optionally the
     * AST for syntax highlight and other information the UI can use to offer help.
     */
    QueryInfo parse(Query query);
}
