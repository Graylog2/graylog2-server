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
import { useQuery } from '@tanstack/react-query';
import uniqWith from 'lodash/uniqWith';
import isEqual from 'lodash/isEqual';

import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';
import type FetchError from 'logic/errors/FetchError';
import { onError } from 'util/conditional/onError';

export type ValueExpr = '>' | '<' | '>=' | '<=' | '==';

export type EventDefinitionAggregation = {
  expr: ValueExpr,
  value: number,
  function: string,
  fnSeries: string,
  field?: string
}
export const definitionsUrl = (definitionId: string) => qualifyUrl(`/events/definitions/${definitionId}`);

const transformExpressionsToArray = ({ series, conditions }): Array<EventDefinitionAggregation> => {
  const res = [];

  const rec = (expression) => {
    if (!expression) {
      return 'No condition configured';
    }

    switch (expression.expr) {
      case 'number':
        return ({ value: expression.value });
      case 'number-ref':
        // eslint-disable-next-line no-case-declarations
        const numberRefSeries = series.find((s) => s.id === expression.ref);

        return (numberRefSeries?.type
          ? { field: `${numberRefSeries.type}(${numberRefSeries.field || ''})` }
          : null);
      case '&&':
      case '||':
        return [rec(expression.left), rec(expression.right)];
      case 'group':
        return [rec(expression.child)];
      case '<':
      case '<=':
      case '>':
      case '>=':
      case '==':
        // eslint-disable-next-line no-case-declarations
        const { ref } = expression.left;
        // eslint-disable-next-line no-case-declarations
        const selectedSeries = series.find((s) => s.id === ref);
        // eslint-disable-next-line no-case-declarations
        const fnSeries = selectedSeries?.type ? `${selectedSeries.type}(${selectedSeries.field || ''})` : undefined;
        res.push({ expr: expression.expr, value: expression.right.value, function: selectedSeries?.type, fnSeries, field: selectedSeries?.field });

        return [rec(expression.left), rec(expression.right)];
      default:
        return null;
    }
  };

  rec(conditions.expression);

  return res;
};

const eventDefinitionDataMapper = (data: EventDefinition): { eventDefinition: EventDefinition, aggregations: Array<EventDefinitionAggregation>} => ({
  eventDefinition: data,
  aggregations: (data?.config?.series && data?.config?.conditions)
    ? uniqWith(transformExpressionsToArray({ series: data.config.series, conditions: data.config.conditions }), isEqual)
    : [],
});

const fetchDefinition = (definitionId: string) => fetch('GET', definitionsUrl(definitionId)).then(eventDefinitionDataMapper);

const useEventDefinition = (definitionId: string, { onErrorHandler }: { onErrorHandler?: (e: FetchError)=>void} = {}): {
  data: { eventDefinition: EventDefinition, aggregations: Array<EventDefinitionAggregation> },
  refetch: () => void,
  isLoading: boolean,
  isFetched: boolean
} => {
  const { data, refetch, isLoading, isFetched } = useQuery(
    ['event-definition-by-id', definitionId],
    () => onError(fetchDefinition(definitionId), (errorThrown: FetchError) => {
      if (onErrorHandler) onErrorHandler(errorThrown);

      UserNotification.error(`Loading event definition failed with status: ${errorThrown}`,
        'Could not load event definition');
    }),
    {
      keepPreviousData: true,
      enabled: !!definitionId,
      initialData: {
        eventDefinition: null,
        aggregations: [],
      },
    },
  );

  return ({
    data,
    refetch,
    isLoading,
    isFetched,
  });
};

export default useEventDefinition;
