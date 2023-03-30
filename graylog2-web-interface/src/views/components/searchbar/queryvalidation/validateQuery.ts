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
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import type { ElasticsearchQueryString, TimeRange } from 'views/logic/queries/Query';
import type { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';
import { onSubmittingTimerange } from 'views/components/TimerangeForForm';
import generateId from 'logic/generateId';

export type ValidationQuery = {
  queryString: ElasticsearchQueryString | string,
  timeRange?: TimeRange | undefined,
  streams?: Array<string>,
  filter?: ElasticsearchQueryString | string,
  validation_mode?: 'QUERY' | 'SEARCH_FILTER'
}

const queryExists = (query: string | ElasticsearchQueryString) => {
  return typeof query === 'object' ? !!query.query_string : !!query;
};

export const validateQuery = (
  {
    queryString,
    timeRange,
    streams,
    filter,
    ...rest
  }: ValidationQuery,
  userTimezone: string,
): Promise<QueryValidationState> => {
  if (!queryExists(queryString) && !queryExists(filter)) {
    return Promise.resolve({ status: 'OK', explanations: [] });
  }

  const payload = {
    query: queryString,
    timerange: timeRange ? onSubmittingTimerange(timeRange, userTimezone) : undefined,
    streams,
    filter,
    ...rest,
  };

  return fetch('POST', qualifyUrl('/search/validate'), payload).then((result) => {
    if (result) {
      const explanations = result.explanations?.map(({
        error_type: errorType,
        error_title: errorTitle,
        error_message: errorMessage,
        begin_line: beginLine,
        end_line: endLine,
        begin_column: beginColumn,
        end_column: endColumn,
        related_property: relatedProperty,
      }) => ({
        id: generateId(),
        errorMessage,
        errorType,
        errorTitle,
        beginLine: beginLine ? beginLine - 1 : 0,
        endLine: endLine ? endLine - 1 : 0,
        beginColumn,
        endColumn,
        relatedProperty,
      } as const));

      return ({
        status: result.status,
        explanations,
      } as const);
    }

    return undefined;
  }).catch((error) => {
    UserNotification.error(`Validating search query failed with status: ${error}`);

    return ({
      status: 'OK',
      explanations: [],
    });
  });
};

export default validateQuery;
