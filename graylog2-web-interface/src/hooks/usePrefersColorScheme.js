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
