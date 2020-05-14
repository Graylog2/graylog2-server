// @flow strict
import * as React from 'react';
import { singleton } from 'views/logic/singleton';
import type { RetrieveColorFunction } from '../visualizations/ChartColorContext';

const ViewColorContext = React.createContext<{ getColor: RetrieveColorFunction }>({
  getColor: () => '#fff',
});
export default singleton('views.components.context.ViewColorContext', () => ViewColorContext);
