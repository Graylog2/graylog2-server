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

import ColorMapper from 'views/components/visualizations/ColorMapper';
import useAppDispatch from 'stores/useAppDispatch';
import { setChartColor } from 'views/logic/slices/widgetActions';

import useColorRules from './useColorRules';

import ChartColorContext from '../visualizations/ChartColorContext';

type Props = {
  children: React.ReactNode,
  id: string,
};

const WidgetColorContext = ({ children, id }: Props) => {
  const colorRules = useColorRules();
  const colorRulesForWidget = useMemo(() => {
    const colorMapperBuilder = ColorMapper.builder();
    const colorRulesForWidgetBuilder = colorRules.filter(({ widgetId }) => (widgetId === id))
      .reduce((prev, { name, color }) => (prev.set(name, color)), colorMapperBuilder);

    return colorRulesForWidgetBuilder.build();
  }, [colorRules, id]);
  const dispatch = useAppDispatch();

  const contextValue = useMemo(() => {
    const setColor = (name: string, color: string) => {
      colorRulesForWidget.set(name, color);

      return dispatch(setChartColor(id, name, color));
    };

    return ({
      colors: colorRulesForWidget,
      setColor,
    });
  }, [colorRulesForWidget, dispatch, id]);

  return (
    <ChartColorContext.Provider value={contextValue}>
      {children}
    </ChartColorContext.Provider>
  );
};

export default WidgetColorContext;
