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
import { useCallback, useMemo } from 'react';

import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { ClickPoint } from 'views/components/visualizations/OnClickPopover/Types';
import type { AddAnnotationFormValues } from 'views/components/visualizations/OnClickPopover/AddAnnotationAction';
import { updateWidgetConfig } from 'views/logic/slices/widgetActions';
import useAppDispatch from 'stores/useAppDispatch';
import type Series from 'views/logic/aggregationbuilder/Series';

type Props = {
  widgetId: string;
  config: AggregationWidgetConfig;
  metric: string;
  clickPoint: ClickPoint;
};

const useUpdateAnnotation = ({ widgetId, metric, config, clickPoint }: Props) => {
  const dispatch = useAppDispatch();

  const updateWidgetSeries = useCallback(
    (updatedSeries: Array<Series>) => {
      const updatedWidgetConfig = config.toBuilder().series(updatedSeries).build();

      return dispatch(updateWidgetConfig(widgetId, updatedWidgetConfig));
    },
    [config, dispatch, widgetId],
  );

  const onAddAnnotation = useCallback(
    ({ note, showReferenceLines, color }: AddAnnotationFormValues) => {
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

      return updateWidgetSeries(updatedSeries);
    },
    [clickPoint.data.x, clickPoint.pointNumber, clickPoint.y, config.series, metric, updateWidgetSeries],
  );

  const onRemoveAnnotation = useCallback(() => {
    const updatedSeries = config.series.map((s) => {
      if (metric !== s.function) return s;

      const curAnnotations = s.config.annotations ?? [];
      const newAnnotations = curAnnotations.filter(
        ({ x, y }) => String(clickPoint.data.x[clickPoint.pointNumber]) !== x && String(clickPoint.y) !== y,
      );

      const seriesConfig = s.config.toBuilder().annotations(newAnnotations).build();

      return s.toBuilder().config(seriesConfig).build();
    });

    return updateWidgetSeries(updatedSeries);
  }, [clickPoint.data.x, clickPoint.pointNumber, clickPoint.y, config.series, metric, updateWidgetSeries]);
  const hasAnnotation = useMemo(
    () =>
      !!config.series
        .find((s) => metric === s.function)
        ?.config?.annotations?.find(
          ({ x, y }) => x === String(clickPoint.data.x[clickPoint.pointNumber]) && y === String(clickPoint.y),
        ),
    [clickPoint.data.x, clickPoint.pointNumber, clickPoint.y, config.series, metric],
  );

  return { hasAnnotation, onAddAnnotation, onRemoveAnnotation };
};

export default useUpdateAnnotation;
