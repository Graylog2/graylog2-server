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
import Sawmill from './Sawmill';

import SawmillMantine from '../mantine/Sawmill';
import { COLOR_SCHEME_DARK, COLOR_SCHEME_LIGHT } from '../THEME_BASE';

describe('styled-components sawmill', () => {
  const CUSTOM_COLORS = {
    variant: {
      danger: '#FF0000',
      default: '#808080',
      info: '#0000FF',
      primary: '#800080',
      success: '#008000',
      warning: '#FFFF00',
    },
    global: {
      background: '#89a4ff',
      contentBackground: '#0b42ff',
      link: '#8cb5ea',
    },
    brand: {
      primary: '#ff1e1b',
      secondary: '#ffebeb',
      tertiary: '#5c6370',
      logo: '#8e99ab',
      concrete: '#93a5c0',
    },
  };

  it('should generate light theme based on custom colors', () => {
    const mantineTheme = SawmillMantine({ colorScheme: COLOR_SCHEME_LIGHT, customColors: CUSTOM_COLORS });
    const theme = Sawmill(mantineTheme);

    expect(theme).toMatchSnapshot();
  });

  it('should generate dark theme based on custom colors', () => {
    const mantineTheme = SawmillMantine({ colorScheme: COLOR_SCHEME_DARK, customColors: CUSTOM_COLORS });
    const theme = Sawmill(mantineTheme);

    expect(theme).toMatchSnapshot();
  });
});
