import React, { useState } from 'react';
import { ThemeProvider } from 'styled-components';
import * as GraylogSawmill from '@graylog/sawmill';
import type { TChangeMode, TThemeMode } from '@graylog/sawmill';

import { DEFAULT_THEME_MODE } from './constants';

type Props = {
  children: React.ReactNode,
};

const Sawmill = GraylogSawmill.default;

const PreflightThemeProvider = ({ children }: Props) => {
  const [mode, setMode] = useState<TThemeMode>(DEFAULT_THEME_MODE);

  const handleModeChange: TChangeMode = (nextMode) => {
    setMode(nextMode);
  };

  const theme = new Sawmill(GraylogSawmill[mode], mode, handleModeChange);

  return (
    <ThemeProvider theme={theme}>
      {children}
    </ThemeProvider>
  );
};

export default PreflightThemeProvider;
