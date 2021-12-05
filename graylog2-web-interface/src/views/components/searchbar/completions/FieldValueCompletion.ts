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
import { isEmpty } from 'lodash';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import type { TimeRange, NoTimeRangeOverride } from 'views/logic/queries/Query';

import type { Token } from '../ace-types';
import type { Completer } from '../SearchBarAutocompletions';

const suggestionsUrl = qualifyUrl('/search/suggest');

const formatValue = (value: string, type: string) => {
  if (type === 'constant.numeric') {
    return Number(value);
  }

  return value;
};

class FieldValueCompletion implements Completer {
  getCompletions = (
    currentToken: Token | undefined | null,
    lastToken: Token | undefined | null,
    prefix: string,
    tokens,
    currentTokenId,
    timeRange?: TimeRange | NoTimeRangeOverride,
    streams?: Array<string>,
  ) => {
    if (lastToken?.type === 'keyword' && (currentToken?.type === 'term' || currentToken?.type === 'constant.numeric')) {
      return fetch('POST', suggestionsUrl, {
        field: lastToken.value.slice(0, -1),
        input: formatValue(currentToken.value, currentToken.type),
        timerange: !isEmpty(timeRange) ? timeRange : undefined,
        streams,
      }).then(({ suggestions }) => {
        return suggestions.map(({ value, occurrence }) => ({ name: value, value, score: occurrence }));
      });
    }

    return Promise.resolve([]);
  };
}

export default FieldValueCompletion;
