import * as React from 'react';
import { useState } from 'react';

import ExportSettingsContext, { ExportSettings } from 'views/components/ExportSettingsContext';

type Props = {
  children: React.ReactNode,
};

const ExportSettingsContextProvider = ({ children }: Props) => {
  const [exportSettings, setExportSettings] = useState<ExportSettings>();

  return (
    <ExportSettingsContext.Provider value={{ settings: exportSettings, setSettings: setExportSettings }}>
      {children}
    </ExportSettingsContext.Provider>
  );
};

export default ExportSettingsContextProvider;
