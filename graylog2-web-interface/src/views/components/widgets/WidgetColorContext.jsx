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
