// @flow strict
import * as React from 'react';
import { singleton } from 'views/logic/singleton';

export type RetrieveColorFunction = (string) => string;
export type ChangeColorFunction = (string, string) => Promise<*>;

const ChartColorContext = React.createContext<{ getColor: RetrieveColorFunction, setColor: ChangeColorFunction }>({
  getColor: () => '#000',
  setColor: () => Promise.resolve([]),
});
export default singleton('views.components.visualizations.ChartColorContext', () => ChartColorContext);
