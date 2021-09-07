import * as React from 'react';

import { VisualizationConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';

export type OnVisualizationConfigChange = (newConfig: VisualizationConfigFormValues) => void;

const OnVisualizationConfigChangeContext = React.createContext<OnVisualizationConfigChange>(() => {});

export default OnVisualizationConfigChangeContext;
