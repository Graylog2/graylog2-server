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
import { useEffect, useState } from 'react';
import styled, { css, useTheme } from 'styled-components';
import defer from 'lodash/defer';

import { Icon, Toggle } from 'components/common';
import {
  COLOR_SCHEME_LIGHT,
  COLOR_SCHEME_DARK,
} from 'theme/constants';

const ThemeModeToggleWrap = styled.div`
  display: flex;
  align-items: center;
`;

const ModeIcon = styled(Icon)<{ $currentMode: boolean }>(({ theme, $currentMode }) => css`
  opacity: ${$currentMode ? '1' : '0.5'};
  color: ${$currentMode ? theme.colors.brand.primary : theme.colors.variant.darkest.default};
`);

const ThemeModeToggle = () => {
  const theme = useTheme();
  const currentMode = theme.mode;
  const [loadingTheme, setLoadingTheme] = useState(false);

  useEffect(() => {
    if (loadingTheme) {
      setLoadingTheme(false);
    }
  }, [loadingTheme, theme]);

  const toggleThemeMode = (event) => {
    const { checked } = event.target;
    event.persist();
    setLoadingTheme(true);
    const newMode = checked ? COLOR_SCHEME_DARK : COLOR_SCHEME_LIGHT;
    defer(() => theme.changeMode(newMode));
  };

  const loadingLightMode = currentMode === COLOR_SCHEME_DARK && loadingTheme;
  const loadingDarkMode = currentMode === COLOR_SCHEME_LIGHT && loadingTheme;

  return (
    <ThemeModeToggleWrap>
      <ModeIcon name={loadingLightMode ? 'spinner' : 'sun'}
                spin={loadingLightMode}
                $currentMode={currentMode === COLOR_SCHEME_LIGHT} />
      <Toggle>
        <input value={COLOR_SCHEME_DARK}
               type="checkbox"
               onChange={toggleThemeMode}
               checked={currentMode === COLOR_SCHEME_DARK}
               disabled={loadingLightMode || loadingDarkMode} />
        <span className="slider" />
      </Toggle>
      <ModeIcon name={loadingDarkMode ? 'spinner' : 'moon'}
                spin={loadingDarkMode}
                $currentMode={currentMode === COLOR_SCHEME_DARK} />
    </ThemeModeToggleWrap>
  );
};

export default ThemeModeToggle;
