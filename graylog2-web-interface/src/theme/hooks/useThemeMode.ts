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
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useTheme as useSCTheme } from 'styled-components';
import defer from 'lodash/defer';

import { COLOR_SCHEME_DARK, COLOR_SCHEME_LIGHT } from 'theme/constants';

const useThemeMode = () => {
  const theme = useSCTheme();
  const currentMode = theme.mode;
  const [loadingTheme, setLoadingTheme] = useState(false);

  useEffect(() => {
    if (loadingTheme) {
      setLoadingTheme(false);
    }
  }, [loadingTheme, theme]);

  const toggleThemeMode = useCallback(() => {
    setLoadingTheme(true);
    const newMode = currentMode === COLOR_SCHEME_LIGHT ? COLOR_SCHEME_DARK : COLOR_SCHEME_LIGHT;
    defer(() => theme.changeMode(newMode));
  }, [currentMode, theme]);

  return useMemo(
    () => ({
      currentMode,
      toggleThemeMode,
      loadingTheme,
    }),
    [currentMode, loadingTheme, toggleThemeMode],
  );
};
export default useThemeMode;
