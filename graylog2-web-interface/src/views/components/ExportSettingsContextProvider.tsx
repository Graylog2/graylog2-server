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
import * as React from 'react';
import { useState, useMemo } from 'react';

import type { ExportSettings } from 'views/components/ExportSettingsContext';
import ExportSettingsContext from 'views/components/ExportSettingsContext';

type Props = {
  children: React.ReactNode,
};

const ExportSettingsContextProvider = ({ children }: Props) => {
  const [exportSettings, setExportSettings] = useState<ExportSettings>();

  const exportSettingsContextValue = useMemo(() => ({
    settings: exportSettings, setSettings: setExportSettings,
  }), [exportSettings]);

  return (
    <ExportSettingsContext.Provider value={exportSettingsContextValue}>
      {children}
    </ExportSettingsContext.Provider>
  );
};

export default ExportSettingsContextProvider;
