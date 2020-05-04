// @flow strict
import * as React from 'react';
import { singleton } from 'views/logic/singleton';

export type ChartColorMap = { [string]: string };
export type ChangeColorFunction = (string, string) => Promise<*>;

const ChartColorContext = React.createContext<{ colors: ChartColorMap, setColor: ChangeColorFunction }>({
  colors: {},
  setColor: () => Promise.resolve([]),
});
export default singleton('views.components.visualizations.ChartColorContext', () => ChartColorContext);
