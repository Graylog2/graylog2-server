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
  console.log({ theme });

  return (
    <ThemeProvider theme={theme}>
      {children}
    </ThemeProvider>
  );
};

export default PreflightThemeProvider;
