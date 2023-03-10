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

import { useEffect } from 'react';

import useAlertAndEventDefinitionData from 'hooks/useAlertAndEventDefinitionData';
import type { ValueExpr } from 'hooks/useEventDefinition';
import { createHighlightingRules } from 'views/logic/slices/highlightActions';
import useAppDispatch from 'stores/useAppDispatch';
import type { Condition } from 'views/logic/views/formatting/highlighting/HighlightingRule';
import { randomColor } from 'views/logic/views/formatting/highlighting/HighlightingRule';

export const exprToConditionMapper: {[name: string]: Condition} = {
  '<': 'less',
  '<=': 'less_equal',
  '>=': 'greater_equal',
  '>': 'greater',
  '==': 'equal',
};

export const conditionToExprMapper: {[name: string]: ValueExpr} = {
  less: '<',
  less_equal: '<=',
  greater_equal: '>=',
  greater: '>',
  equal: '==',
};

const useHighlightValuesForEventDefinition = () => {
  const dispatch = useAppDispatch();
  const { aggregations } = useAlertAndEventDefinitionData();

  return useEffect(() => {
    if (aggregations?.length) {
      const valuesToHighlight = aggregations.map(({ fnSeries, value, expr }) => ({
        value,
        field: fnSeries,
        color: randomColor(),
        condition: exprToConditionMapper[expr],
      }));
      if (valuesToHighlight.length) dispatch(createHighlightingRules(valuesToHighlight));
    }
  }, [aggregations, dispatch]);
};

export default useHighlightValuesForEventDefinition;
