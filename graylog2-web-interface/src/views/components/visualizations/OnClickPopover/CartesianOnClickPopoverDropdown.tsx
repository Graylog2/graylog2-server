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
import React, { useMemo, useCallback } from 'react';
import styled, { css } from 'styled-components';

import Popover from 'components/common/Popover';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import { keySeparator } from 'views/Constants';
import OnClickPopoverValueGroups from 'views/components/visualizations/OnClickPopover/OnClickPopoverValueGroups';
import type { ValueGroups, ClickPoint } from 'views/components/visualizations/OnClickPopover/Types';
import getHoverSwatchColor from 'views/components/visualizations/utils/getHoverSwatchColor';
import Button from 'components/bootstrap/Button';
import useAppDispatch from 'stores/useAppDispatch';
import useWidget from 'views/hooks/useWidget';
import { updateWidgetConfig } from 'views/logic/slices/widgetActions';
import type { AddAnnotationFormValues } from 'views/components/visualizations/OnClickPopover/AddAnnotationAction';
import AddAnnotationAction from 'views/components/visualizations/OnClickPopover/AddAnnotationAction';

const DivContainer = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.xxs};
  `,
);

const CartesianOnClickPopoverDropdown = ({
  clickPoint,
  config,
  widgetId,
}: {
  clickPoint: ClickPoint;
  config: AggregationWidgetConfig;
  widgetId: string;
}) => {
  const traceColor = getHoverSwatchColor(clickPoint);
  const dispatch = useAppDispatch();

  const { rowPivotValues, columnPivotValues, metricValue } = useMemo<ValueGroups>(() => {
    if (!clickPoint || !config) return {};
    const splitNames: Array<string | number> = (clickPoint.data.originalName ?? clickPoint.data.name).split(
      keySeparator,
    );
    const metric: string = splitNames.pop() as string;

    const columnPivotsToFields = config?.columnPivots?.flatMap((pivot) => pivot.fields) ?? [];

    const rowPivotsToFields = config?.rowPivots?.flatMap((pivot) => pivot.fields) ?? [];
    const splitXValues: Array<string | number> = `${String(clickPoint.x)}`.split(keySeparator);

    return {
      rowPivotValues: splitXValues.map((value, i) => ({
        value,
        field: rowPivotsToFields[i],
        text: String(value),
        traceColor,
      })),
      columnPivotValues: splitNames.map((value, i) => ({
        value,
        field: columnPivotsToFields[i],
        text: String(value),
        traceColor,
      })),
      metricValue: {
        value: clickPoint.y,
        field: metric,
        text: `${String(clickPoint.text ?? clickPoint.y)}`,
        traceColor,
      },
    };
  }, [clickPoint, config, traceColor]);

  const onAddAnnotation = useCallback(
    ({ note, showReferenceLines, color }: AddAnnotationFormValues) => {
      const metric: string = metricValue.field;
      const updatedSeries = config.series.map((s) => {
        if (metric !== s.function) return s;

        const curAnnotations = s.config.annotations ?? [];
        const newAnnotations = [
          ...curAnnotations,
          {
            note,
            x: String(clickPoint.data.x[clickPoint.pointNumber]),
            y: String(clickPoint.y),
            color,
            showReferenceLines,
          },
        ];
        const seriesConfig = s.config.toBuilder().annotations(newAnnotations).build();

        return s.toBuilder().config(seriesConfig).build();
      });

      const updatedWidgetConfig = config.toBuilder().series(updatedSeries).build();

      console.log({ updatedWidgetConfig, widgetId });

      return dispatch(updateWidgetConfig(widgetId, updatedWidgetConfig));
    },
    [clickPoint, config, dispatch, metricValue.field, widgetId],
  );

  return (
    <Popover.Dropdown>
      <DivContainer>
        <OnClickPopoverValueGroups
          columnPivotValues={columnPivotValues}
          metricValue={metricValue}
          rowPivotValues={rowPivotValues}
        />
        <AddAnnotationAction onAddAnnotation={onAddAnnotation} />
      </DivContainer>
    </Popover.Dropdown>
  );
};

export default CartesianOnClickPopoverDropdown;
