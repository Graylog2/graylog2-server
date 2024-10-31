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
import * as React from 'react';
import styled from 'styled-components';
import { useMemo, useCallback } from 'react';

import { StaticColor } from 'views/logic/views/formatting/highlighting/HighlightingColor';
import { ColorPickerPopover, Icon } from 'components/common';
import { DEFAULT_CUSTOM_HIGHLIGHT_RANGE } from 'views/Constants';
import type HighlightingRule from 'views/logic/views/formatting/highlighting/HighlightingRule';
import { conditionToExprMapper, exprToConditionMapper } from 'views/logic/ExpressionConditionMappers';
import useAppSelector from 'stores/useAppSelector';
import { selectHighlightingRules } from 'views/logic/slices/highlightSelectors';
import useAlertAndEventDefinitionData from 'hooks/useAlertAndEventDefinitionData';
import { updateHighlightingRule, createHighlightingRules } from 'views/logic/slices/highlightActions';
import { randomColor } from 'views/logic/views/formatting/highlighting/HighlightingRule';
import useAppDispatch from 'stores/useAppDispatch';
import NoAttributeProvided from 'components/event-definitions/replay-search/NoAttributeProvided';

const List = styled.div`
  display: flex;
  gap: 5px;
`;

const Condition = styled.div`
  display: flex;
  gap: 5px;
  align-items: center;
`;

const ColorComponent = styled.div`
  width: 13px;
  height: 13px;
  border-radius: 2px;
  cursor: pointer;
`;

const useHighlightingRules = () => useAppSelector(selectHighlightingRules);

const AggregationConditions = () => {
  const dispatch = useAppDispatch();
  const { aggregations } = useAlertAndEventDefinitionData();
  const highlightingRules = useHighlightingRules();

  const aggregationsMap = useMemo(() => new Map(aggregations.map((agg) => [
    `${agg.fnSeries}${agg.expr}${agg.value}`, agg,
  ])), [aggregations]);

  const changeColor = useCallback(({ rule, newColor, condition }) => {
    if (rule) {
      dispatch(updateHighlightingRule(rule, { color: StaticColor.create(newColor) }));
    } else {
      const { value, fnSeries, expr } = aggregationsMap.get(condition);

      dispatch(createHighlightingRules([
        {
          value,
          field: fnSeries,
          color: randomColor(),
          condition: exprToConditionMapper[expr],
        },
      ]));
    }
  }, [aggregationsMap, dispatch]);

  const highlightedAggregations = useMemo<Map<string, HighlightingRule>>(() => {
    const initial = new Map<string, HighlightingRule>(aggregations.map(
      ({ fnSeries, value, expr }) => [
        `${fnSeries}${expr}${value}`, undefined,
      ],
    ));

    return highlightingRules.reduce((acc, rule) => {
      const { field, value, condition } = rule;
      const expr = conditionToExprMapper?.[condition];
      let result = acc;

      if (expr) {
        const key = `${field}${expr}${value}`;

        if (acc.has(key)) {
          result = result.set(key, rule);
        }
      }

      return result;
    }, initial);
  }, [aggregations, highlightingRules]);

  return highlightedAggregations?.size ? (
    <List>
      {Array.from(highlightedAggregations).map(([condition, rule]) => {
        const color = rule?.color as StaticColor;
        const hexColor = color?.color;

        return (
          <Condition title={condition} key={condition}>
            <ColorPickerPopover id="formatting-rule-color"
                                placement="right"
                                color={hexColor}
                                colors={DEFAULT_CUSTOM_HIGHLIGHT_RANGE.map((c) => [c])}
                                triggerNode={(
                                  <ColorComponent style={{ backgroundColor: hexColor }}>
                                    {!hexColor && <Icon name="colors" size="xs" />}
                                  </ColorComponent>
                                )}
                                onChange={(newColor, _, hidePopover) => {
                                  hidePopover();
                                  changeColor({ newColor, rule, condition });
                                }} />
            <span>{condition}</span>
          </Condition>
        );
      })}
    </List>
  ) : <NoAttributeProvided name="Aggregation conditions" />;
};

export default AggregationConditions;
