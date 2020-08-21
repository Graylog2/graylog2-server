const prefersDarkMode = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;

const PREFERENCES_THEME_MODE = 'themeMode';
const THEME_MODE_LIGHT = 'teint';
const THEME_MODE_DARK = 'noir';
const THEME_MODES = [THEME_MODE_LIGHT, THEME_MODE_DARK];
const DEFAULT_THEME_MODE = prefersDarkMode ? THEME_MODE_DARK : THEME_MODE_LIGHT;

export {
  THEME_MODES,
  DEFAULT_THEME_MODE,
  PREFERENCES_THEME_MODE,
  THEME_MODE_LIGHT,
  THEME_MODE_DARK,
};
