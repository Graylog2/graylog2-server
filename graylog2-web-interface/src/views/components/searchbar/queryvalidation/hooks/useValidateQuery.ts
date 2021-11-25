/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import { isEmpty, debounce } from 'lodash';
import { useEffect } from 'react';
import { useQuery } from 'react-query';

import { useStore } from 'stores/connect';
import { SearchExecutionStateStore } from 'views/stores/SearchExecutionStateStore';
import { SearchStore } from 'views/stores/SearchStore';
import type { ElasticsearchQueryString, TimeRange } from 'views/logic/queries/Query';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import { NoTimeRangeOverride } from 'views/logic/queries/Query';

import { QueryValidationState } from '../QueryValidation';

const validateQuery = ({ queryString, timeRange, streams, parameters, parameterBindings, filter }) => {
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
      return ({
        status: result.status,
        explanations: result.explanations?.map(({
          error_type: errorType,
          error_message: errorMessage,
          begin_line: beginLine,
          end_line: endLine,
          begin_column: beginColumn,
          end_column: endColumn,
        }) => ({
          errorMessage,
          errorType,
          beginLine,
          endLine,
          beginColumn,
          endColumn,
        })),
      });
    }

    return undefined;
  });
};

const queryExists = (query: string | ElasticsearchQueryString) => {
  return typeof query === 'object' ? !!query.query_string : !!query;
};

const useValidationPayload = ({
  queryString,
  timeRange,
  streams,
  filter,
}: {
  queryString: string | ElasticsearchQueryString | undefined,
  timeRange?: TimeRange | NoTimeRangeOverride | undefined,
  streams?: Array<string>,
  filter?: string | ElasticsearchQueryString
}) => {
  const { parameterBindings } = useStore(SearchExecutionStateStore);
  const { search: { parameters } } = useStore(SearchStore);

  return ({
    timeRange: !isEmpty(timeRange) ? timeRange : undefined,
    filter,
    queryString,
    streams,
    parameters,
    parameterBindings,
  });
};

const debouncedRefetch = debounce((refetch) => {
  refetch({ cancelRefetch: true });
}, 350);

const useValidateQuery = (queryData: {
  queryString: string | ElasticsearchQueryString | undefined,
  timeRange?: TimeRange | NoTimeRangeOverride | undefined,
  streams?: Array<string>,
  filter?: string | ElasticsearchQueryString
}): QueryValidationState | undefined => {
  const { queryString, timeRange, streams, filter, parameterBindings, parameters } = useValidationPayload(queryData);

  const { data: validationState, refetch, remove } = useQuery(
    'validateSearchQuery',
    () => validateQuery({ queryString, timeRange, streams, parameters, parameterBindings, filter }),
    { enabled: false },
  );

  useEffect(() => {
    if (queryExists(queryString) || queryExists(filter)) {
      debouncedRefetch(refetch);
    }
  }, [filter, queryString, refetch]);

  useEffect(() => {
    if (!queryExists(queryString) && !queryExists(filter) && validationState) {
      remove();
    }
  }, [queryString, filter, validationState, remove]);

  return validationState;
};

export default useValidateQuery;
