// @flow strict
import * as React from 'react';
import { useContext } from 'react';
import PropTypes from 'prop-types';

import connect from 'stores/connect';
import { ChartColorRulesStore, ChartColorRulesActions } from 'views/stores/ChartColorRulesStore';
import type { ColorRule } from 'views/stores/ChartColorRulesStore';
import ChartColorContext from '../visualizations/ChartColorContext';
import ViewColorContext from '../contexts/ViewColorContext';

type Props = {
  children: React.Node,
  colorRules: Array<ColorRule>,
  id: string,
};

const WidgetColorContext = ({ children, colorRules, id }: Props) => {
  const viewColorContext = useContext(ViewColorContext);
  const colorRulesForWidget = colorRules.filter(({ widgetId }) => (widgetId === id))
    .reduce((prev, { name, color }) => ({ ...prev, [name]: color }), {});
  const getColor = (trace) => colorRulesForWidget[trace] ?? viewColorContext.getColor(trace);
  const setColor = (name, color) => ChartColorRulesActions.set(id, name, color);

  return (
    <ChartColorContext.Provider value={{ getColor, setColor }}>
      {children}
    </ChartColorContext.Provider>
  );
};

WidgetColorContext.propTypes = {
  children: PropTypes.node.isRequired,
  id: PropTypes.string.isRequired,
};

export default connect(WidgetColorContext, { colorRules: ChartColorRulesStore });
