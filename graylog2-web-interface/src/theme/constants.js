const prefersDarkMode = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;

const THEME_MODES = ['teint', 'noir', 'partyGorillaLight', 'partyGorillaDark'];

const PREFERENCES_THEME_MODE = 'themeMode';
const THEME_MODE_LIGHT = 'partyGorillaLight';
const THEME_MODE_DARK = 'partyGorillaDark';
const DEFAULT_THEME_MODE = prefersDarkMode ? THEME_MODE_DARK : THEME_MODE_LIGHT;

export {
  DEFAULT_THEME_MODE,
  PREFERENCES_THEME_MODE,
  THEME_MODE_LIGHT,
  THEME_MODE_DARK,
  THEME_MODES,
};
