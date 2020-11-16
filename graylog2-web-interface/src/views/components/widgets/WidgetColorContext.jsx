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
// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import connect from 'stores/connect';
import { ChartColorRulesStore, ChartColorRulesActions } from 'views/stores/ChartColorRulesStore';
import type { ColorRule } from 'views/stores/ChartColorRulesStore';

import ChartColorContext from '../visualizations/ChartColorContext';

type Props = {
  children: React.Node,
  colorRules: Array<ColorRule>,
  id: string,
};

const WidgetColorContext = ({ children, colorRules, id }: Props) => {
  const colorRulesForWidget = colorRules.filter(({ widgetId }) => (widgetId === id))
    .reduce((prev, { name, color }) => ({ ...prev, [name]: color }), {});
  const setColor = (name, color) => ChartColorRulesActions.set(id, name, color);
  const contextValue = { colors: colorRulesForWidget, setColor };

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

export default connect(WidgetColorContext, { colorRules: ChartColorRulesStore });
