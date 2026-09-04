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
import styled, { css } from 'styled-components';

import { Icon } from 'components/common';
import { COLOR_SCHEME_LIGHT, COLOR_SCHEME_DARK } from 'theme/constants';
import Switch from 'components/common/Switch';
import useThemeMode from 'theme/hooks/useThemeMode';

const ThemeModeToggleWrap = styled.div`
  display: flex;
  align-items: center;
  gap: 6px;
`;

const ModeIcon = styled(Icon)<{ $currentMode: boolean }>(
  ({ theme, $currentMode }) => css`
    opacity: ${$currentMode ? '1' : '0.5'};
    color: ${$currentMode ? theme.colors.variant.danger : theme.colors.variant.darkest.default};
  `,
);

const ThemeModeToggle = () => {
  const { currentMode, toggleThemeMode, loadingTheme } = useThemeMode();
  const loadingLightMode = currentMode === COLOR_SCHEME_DARK && loadingTheme;
  const loadingDarkMode = currentMode === COLOR_SCHEME_LIGHT && loadingTheme;

  return (
    <ThemeModeToggleWrap>
      <ModeIcon
        name={loadingLightMode ? 'progress_activity' : 'light_mode'}
        spin={loadingLightMode}
        $currentMode={currentMode === COLOR_SCHEME_LIGHT}
      />
      <Switch
        checked={currentMode === COLOR_SCHEME_DARK}
        disabled={loadingLightMode || loadingDarkMode}
        onChange={toggleThemeMode}
      />
      <ModeIcon
        name={loadingDarkMode ? 'progress_activity' : 'dark_mode'}
        spin={loadingDarkMode}
        $currentMode={currentMode === COLOR_SCHEME_DARK}
      />
    </ThemeModeToggleWrap>
  );
};

export default ThemeModeToggle;
