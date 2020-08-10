import { useState, useEffect } from 'react';

// * https://developer.mozilla.org/en-US/docs/Web/API/Window/matchMedia
// * https://developer.mozilla.org/en-US/docs/Web/API/MediaQueryList
// * https://developer.mozilla.org/en-US/docs/Web/CSS/Media_Queries/Testing_media_queries

export default function useMatchMedia(query) {
  const mediaQueryList = window.matchMedia(query);
  const [match, setMatch] = useState(mediaQueryList.matches);

  useEffect(() => {
    const handleMatchChange = (ev) => setMatch(ev.matches);

    mediaQueryList.addListener(handleMatchChange);

    return () => {
      mediaQueryList.removeListener(handleMatchChange);
    };
  }, [query]);

  return match;
}

export const usePrefersColorScheme = () => {
  const mqlLight = window.matchMedia('(prefers-color-scheme: light)');
  const mqlDark = window.matchMedia('(prefers-color-scheme: dark)');
  const prefersLight = mqlLight.matches ? 'light' : null;
  const prefersScheme = mqlDark.matches ? 'dark' : prefersLight;
  const [scheme, setScheme] = useState(prefersScheme);

  useEffect(() => {
    if (window.matchMedia) {
      const handleLight = (ev) => {
        if (ev.matches) {
          setScheme('light');
        }
      };

      const handleDark = (ev) => {
        if (ev.matches) {
          setScheme('dark');
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
