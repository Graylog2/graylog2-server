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
import { useState, useEffect } from 'react';
import type { ColorScheme } from '@graylog/sawmill';
import { COLOR_SCHEME_DARK, COLOR_SCHEME_LIGHT } from '@graylog/sawmill';

// * https://developer.mozilla.org/en-US/docs/Web/API/Window/matchMedia
// * https://developer.mozilla.org/en-US/docs/Web/API/MediaQueryList
// * https://developer.mozilla.org/en-US/docs/Web/CSS/Media_Queries/Testing_media_queries
// * https://developer.mozilla.org/en-US/docs/Web/CSS/@media/prefers-color-scheme

const useBrowserColorSchemePreference = () => {
  if (!window.matchMedia) {
    return null;
  }

  const mqlLight = window.matchMedia('(prefers-color-scheme: light)');
  const mqlDark = window.matchMedia('(prefers-color-scheme: dark)');

  const prefersScheme = mqlDark.matches ? COLOR_SCHEME_DARK : COLOR_SCHEME_LIGHT;
  // eslint-disable-next-line react-hooks/rules-of-hooks
  const [scheme, setScheme] = useState<ColorScheme>(prefersScheme);

  // eslint-disable-next-line react-hooks/rules-of-hooks
  useEffect(() => {
    if (window.matchMedia) {
      const handleLight = (ev: MediaQueryListEvent) => {
        if (ev.matches) {
          setScheme(COLOR_SCHEME_LIGHT);
        }
      };

      const handleDark = (ev: MediaQueryListEvent) => {
        if (ev.matches) {
          setScheme(COLOR_SCHEME_DARK);
        }
      };

      mqlLight.addListener(handleLight);
      mqlDark.addListener(handleDark);

      return () => {
        mqlLight.removeListener(handleLight);
        mqlDark.removeListener(handleDark);
      };
    }

    return null;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return scheme;
};

export default useBrowserColorSchemePreference;
