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
import { useMemo } from 'react';
import PropTypes from 'prop-types';

import { ChartColorRulesActions } from 'views/stores/ChartColorRulesStore';
import type { ColorRule } from 'views/stores/ChartColorRulesStore';
import ColorMapper from 'views/components/visualizations/ColorMapper';
import useWidgets from 'views/hooks/useWidgets';
import type WidgetConfig from 'views/logic/widgets/WidgetConfig';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import WidgetFormattingSettings from 'views/logic/aggregationbuilder/WidgetFormattingSettings';

import ChartColorContext from '../visualizations/ChartColorContext';

type Props = {
  children: React.ReactNode,
  id: string,
};

const isAggregationWidgetConfig = (config: WidgetConfig): config is AggregationWidgetConfig => config && 'formattingSettings' in config;

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

const WidgetColorContext = ({ children, id }: Props) => {
  const colorRules = useColorRules();
  const colorMapperBuilder = ColorMapper.builder();
  const colorRulesForWidgetBuilder = colorRules.filter(({ widgetId }) => (widgetId === id))
    .reduce((prev, { name, color }) => (prev.set(name, color)), colorMapperBuilder);
  const colorRulesForWidget = colorRulesForWidgetBuilder.build();

  const contextValue = useMemo(() => {
    const setColor = (name: string, color: string) => {
      colorRulesForWidget.set(name, color);

      return ChartColorRulesActions.set(id, name, color);
    };

    return ({
      colors: colorRulesForWidget,
      setColor,
    });
  }, [colorRulesForWidget, id]);

  return (
    <ChartColorContext.Provider value={contextValue}>
      {children}
    </ChartColorContext.Provider>
  );
};

WidgetColorContext.propTypes = {
  children: PropTypes.node.isRequired,
  id: PropTypes.string.isRequired,
};

export default WidgetColorContext;
