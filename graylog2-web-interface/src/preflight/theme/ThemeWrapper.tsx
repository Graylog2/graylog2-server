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
import React, { useMemo } from 'react';
import { createGlobalStyle, useTheme } from 'styled-components';
import { MantineProvider } from '@mantine/core';
import SawmillMantine from '@graylog/sawmill/mantine';

type Props = {
  children: React.ReactElement,
};

const GlobalStyles = createGlobalStyle`
  body {
    background-color: ${(props) => props.theme.colors.global.background};
    color: ${(props) => props.theme.colors.global.textDefault};
  },
`;

const ThemeWrapper = ({ children }: Props) => {
  const theme = useTheme();
  const mantineTheme = useMemo(
    () => SawmillMantine({ colorScheme: theme.mode }),
    [theme.mode],
  );

  return (
    <MantineProvider theme={mantineTheme}>
      <GlobalStyles />
      {children}
    </MantineProvider>
  );
};

export default ThemeWrapper;
