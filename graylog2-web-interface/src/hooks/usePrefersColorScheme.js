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

import { THEME_MODE_DARK, THEME_MODE_LIGHT } from 'theme/constants';

// * https://developer.mozilla.org/en-US/docs/Web/API/Window/matchMedia
// * https://developer.mozilla.org/en-US/docs/Web/API/MediaQueryList
// * https://developer.mozilla.org/en-US/docs/Web/CSS/Media_Queries/Testing_media_queries
// * https://developer.mozilla.org/en-US/docs/Web/CSS/@media/prefers-color-scheme

const usePrefersColorScheme = () => {
  if (!window.matchMedia) {
    return null;
  }

  const mqlLight = window.matchMedia('(prefers-color-scheme: light)');
  const mqlDark = window.matchMedia('(prefers-color-scheme: dark)');

  const prefersScheme = mqlDark.matches ? THEME_MODE_DARK : THEME_MODE_LIGHT;
  const [scheme, setScheme] = useState(prefersScheme);

  useEffect(() => {
    if (window.matchMedia) {
      const handleLight = (ev) => {
        if (ev.matches) {
          setScheme(THEME_MODE_LIGHT);
        }
      };

      const handleDark = (ev) => {
        if (ev.matches) {
          setScheme(THEME_MODE_DARK);
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
  }, []);

  return scheme;
};

export default usePrefersColorScheme;
