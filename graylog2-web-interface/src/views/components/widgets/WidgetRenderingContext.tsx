import * as React from 'react';

import { singleton } from 'logic/singleton';

type WidgetRendering = {
  limitHeight: boolean,
};

const defaultWidgetRendering = {
  limitHeight: true,
};
const WidgetRenderingContext = React.createContext<WidgetRendering>(defaultWidgetRendering);
export default singleton('contexts.WidgetRenderingContext', () => WidgetRenderingContext);
