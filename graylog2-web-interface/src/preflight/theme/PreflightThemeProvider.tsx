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
import type { ColorScheme } from '@graylog/sawmill';
import SawmillSC from '@graylog/sawmill/styled-components';
import { MantineProvider } from '@mantine/core';
import type { MantineTheme } from '@graylog/sawmill/mantine';
import SawmillMantine from '@graylog/sawmill/mantine';

import { DEFAULT_THEME_MODE } from './constants';

type Props = {
  children: React.ReactNode,
};

const useSCTheme = (
  setColorScheme: (newColorScheme: ColorScheme) => void,
  mantineTheme: MantineTheme,
) => useMemo(() => {
  const theme = SawmillSC(mantineTheme);

  const onChangeColorScheme = (nextMode: ColorScheme) => {
    setColorScheme(nextMode);
  };

  return ({
    ...theme,
    changeMode: onChangeColorScheme,
  });
}, [mantineTheme, setColorScheme]);

const PreflightThemeProvider = ({ children }: Props) => {
  const [colorScheme, setColorScheme] = useState<ColorScheme>(DEFAULT_THEME_MODE);
  const mantineTheme = useMemo(
    () => SawmillMantine({ colorScheme }),
    [colorScheme],
  );
  const scTheme = useSCTheme(setColorScheme, mantineTheme);

  return (
    <MantineProvider theme={mantineTheme}>
      <ThemeProvider theme={scTheme}>
        {children}
      </ThemeProvider>
    </MantineProvider>
  );
};

export default PreflightThemeProvider;
