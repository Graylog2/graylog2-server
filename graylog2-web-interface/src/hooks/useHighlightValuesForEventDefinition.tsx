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

import { useMemo } from 'react';
import uniqWith from 'lodash/uniqWith';
import isEqual from 'lodash/isEqual';

import useAlertAndEventDefinitionData from 'hooks/useAlertAndEventDefinitionData';
import useSearchResult from 'views/hooks/useSearchResult';
import useCurrentQueryId from 'views/logic/queries/useCurrentQueryId';
import type { EventDefinitionAggregation } from 'hooks/useEventDefinition';
import { randomColor } from 'views/logic/views/formatting/highlighting/HighlightingRule';

const isValuePass = ({ aggregations, key, value }: {aggregations: Array<EventDefinitionAggregation>, key: string, value: number }): boolean => {
  return aggregations.some(({ expr, value: exprValue, fnSeries }: EventDefinitionAggregation) => {
    if (key !== fnSeries) return false;

    switch (expr) {
      case '<':
        return value < exprValue;
      case '>':
        return value > exprValue;
      case '>=':
        return value >= exprValue;
      case '<=':
        return value <= exprValue;
      case '==':
        return value === exprValue;
      default:
        return false;
    }
  });
};

const useHighlightValuesForEventDefinition = () => {
  const searchResult = useSearchResult();
  const { aggregations, isEvent, isAlert, isEventDefinition } = useAlertAndEventDefinitionData();
  const shouldRunHook = isEvent || isAlert || isEventDefinition;
  const curQueryId = useCurrentQueryId();
  const searchTypes: { [name: string]: { name: string, rows: Array<{ values: Array<{ key: string, value: number }> }>}} = searchResult?.result?.result?.results?.[curQueryId]?.search_types;

  return useMemo<Array<{field: string, value: number, colo: string}>>(() => {
    if (!searchTypes || !shouldRunHook) return [];
    const color = randomColor();

    return uniqWith(Object.values(searchTypes).reduce((res, { rows, name }) => {
      if (name !== 'chart') return res;

      rows.forEach((row) => {
        row.values.forEach(({ key, value }) => {
          if (isValuePass({ aggregations, key: key[0], value })) {
            res.push({ field: key[0], value, color });
          }
        });
      });

      return res;
    }, []), isEqual);
  }, [aggregations, searchTypes, shouldRunHook]);
};

export default useHighlightValuesForEventDefinition;
