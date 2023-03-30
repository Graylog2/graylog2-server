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
import useWidgets from 'views/hooks/useWidgets';
import WidgetFormattingSettings from 'views/logic/aggregationbuilder/WidgetFormattingSettings';
import type WidgetConfig from 'views/logic/widgets/WidgetConfig';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

const isAggregationWidgetConfig = (config: WidgetConfig): config is AggregationWidgetConfig => config && 'formattingSettings' in config;

type Color = string;

export type ColorRule = {
  widgetId: string,
  name: string,
  color: Color,
};

const useColorRules = () => {
  const widgets = useWidgets();

  return widgets.valueSeq()
    .toArray()
    .flatMap((widget) => {
      const { config } = widget;
      const widgetId = widget.id;

      if (isAggregationWidgetConfig(config)) {
        const { chartColors = {} } = config.formattingSettings ?? WidgetFormattingSettings.empty();

        return Object.entries(chartColors).map(([key, value]) => ({ widgetId, name: key, color: value } as ColorRule));
      }

      return [];
    })
    .filter((entry) => entry !== null);
};

export default useColorRules;
