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
import React, { useMemo, useState } from 'react';
import { ThemeProvider } from 'styled-components';
import { MantineProvider } from '@mantine/core';

import type { ColorScheme } from 'theme/sawmill';
import SawmillSC from 'theme/sawmill/styled-components/Sawmill';
import type { MantineTheme } from 'theme/sawmill/mantine/Sawmill';
import SawmillMantine from 'theme/sawmill/mantine/Sawmill';

import { DEFAULT_THEME_MODE } from './constants';

type Props = {
  children: React.ReactNode;
};

const useSCTheme = (setColorScheme: (newColorScheme: ColorScheme) => void, mantineTheme: MantineTheme) =>
  useMemo(() => {
    const theme = SawmillSC(mantineTheme);

    const onChangeColorScheme = (nextMode: ColorScheme) => {
      setColorScheme(nextMode);
    };

    return {
      ...theme,
      changeMode: onChangeColorScheme,
    };
  }, [mantineTheme, setColorScheme]);

const PreflightThemeProvider = ({ children }: Props) => {
  const [colorScheme, setColorScheme] = useState<ColorScheme>(DEFAULT_THEME_MODE);
  const mantineTheme = useMemo(() => SawmillMantine({ colorScheme }), [colorScheme]);
  const scTheme = useSCTheme(setColorScheme, mantineTheme);

  return (
    <MantineProvider theme={mantineTheme} forceColorScheme={colorScheme}>
      <ThemeProvider theme={scTheme}>{children}</ThemeProvider>
    </MantineProvider>
  );
};

export default PreflightThemeProvider;
