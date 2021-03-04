import * as React from 'react';

import { singleton } from 'views/logic/singleton';

type ExportSettingsContextType = {
  settings: ExportSettings;
  setSettings: (settings: ExportSettings) => void;
}

export interface ExportSettings {}

const ExportSettingsContext = React.createContext<ExportSettingsContextType>({ settings: {}, setSettings: () => {} });

export default singleton('contexts.ExportSettingsContext', () => ExportSettingsContext);
