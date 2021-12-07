import * as Immutable from 'immutable';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import { ElasticsearchQueryString, TimeRange, NoTimeRangeOverride } from 'views/logic/queries/Query';
import Parameter from 'views/logic/parameters/Parameter';
import { ParameterBindings } from 'views/logic/search/SearchExecutionState';
import type { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';

type ValidationQuery = {
  queryString: ElasticsearchQueryString | string,
  timeRange: TimeRange | NoTimeRangeOverride,
  streams?: Array<string>,
  parameters: Immutable.Set<Parameter>,
  parameterBindings: ParameterBindings,
  filter?: ElasticsearchQueryString | string,
}

const queryExists = (query: string | ElasticsearchQueryString) => {
  return typeof query === 'object' ? !!query.query_string : !!query;
};

export const validateQuery = ({
  queryString,
  timeRange,
  streams,
  parameters,
  parameterBindings,
  filter,
}: ValidationQuery): Promise<QueryValidationState> => {
  if (!queryExists(queryString) && !queryExists(filter)) {
    return Promise.resolve({ status: 'OK', explanations: [] });
  }

  const payload = {
    query: queryString,
    timerange: timeRange,
    streams,
    filter,
    parameters,
    parameter_bindings: parameterBindings,
  };

  return fetch('POST', qualifyUrl('/search/validate'), payload).then((result) => {
    if (result) {
      const explanations = result.explanations?.map(({
        error_type: errorType,
        error_message: errorMessage,
        begin_line: beginLine,
        end_line: endLine,
        begin_column: beginColumn,
        end_column: endColumn,
      }) => ({
        errorMessage,
        errorType,
        beginLine: beginLine ? beginLine - 1 : 0,
        endLine: endLine ? endLine - 1 : 0,
        beginColumn,
        endColumn,
      }));

      return ({
        status: result.status,
        explanations,
      });
    }

    return undefined;
  });
};

export default validateQuery;
