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
import pickBy from 'lodash/pickBy';
import isArray from 'lodash/isArray';
import isNumber from 'lodash/isNumber';
import type Immutable from 'immutable';

import type { MappedData } from 'views/logic/valueactions/createEventDefinition/types';
import type { ActionContexts } from 'views/types';
import type { RelativeTimeRangeWithEnd } from 'views/logic/queries/Query';
import type { ActionComponentProps } from 'views/components/actions/ActionHandler';
import {
  getAggregationHandler,
  getLutParameters,
  getRestParameterValues,
  transformSearchFiltersToQuery,
  replaceParametersInQueryString,
  getStreams,
} from 'views/logic/valueactions/createEventDefinition/hooks/hookHelpers';
import type ValueParameter from 'views/logic/parameters/ValueParameter';
import type LookupTableParameter from 'views/logic/parameters/LookupTableParameter';

type HookProps = Pick<ActionComponentProps, 'field' | 'queryId' | 'value'> & { contexts: ActionContexts }
const useMappedData = ({ contexts, field, queryId, value }: HookProps) => useMemo<MappedData>(() => {
  const aggregationHandler = getAggregationHandler({ widget: contexts.widget, field: field });
  const curQuery = contexts.view.search.queries.find((query) => query.id === queryId);
  const { parameters, parameterBindings } = contexts;
  const searchWithinMs = curQuery.timerange.type === 'relative' ? (curQuery.timerange as RelativeTimeRangeWithEnd).from * 1000 : undefined;
  const lutParameters = getLutParameters(parameters);
  const restParameterValues = getRestParameterValues({ parameters: parameters as Immutable.Set<ValueParameter | LookupTableParameter>, parameterBindings });
  const searchFilterQuery = transformSearchFiltersToQuery(curQuery.filters);
  const queryWithReplacedParams = replaceParametersInQueryString({ query: curQuery.query.query_string, restParameterValues });
  const streams = getStreams(curQuery.filter);
  const { ...aggregationVales } = aggregationHandler({ valuePath: contexts.valuePath, widget: contexts.widget, value: value, field: field });
  const data: MappedData = {
    searchWithinMs,
    lutParameters,
    searchFilterQuery,
    queryWithReplacedParams,
    streams,
    ...aggregationVales,
  };

  return pickBy(data, (v) => {
    if (isArray(v)) {
      return !!v.length;
    }

    if (isNumber(v)) {
      return true;
    }

    return !!v?.trim();
  });
}, [contexts, field, queryId, value]);

export default useMappedData;
