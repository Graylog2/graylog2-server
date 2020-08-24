// @flow strict
const prefersDarkMode = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;

export type ThemeMode = 'teint' | 'noir';
export type PreferencesThemeMode = 'themeMode';

const PREFERENCES_THEME_MODE: PreferencesThemeMode = 'themeMode';
const THEME_MODE_LIGHT = 'teint';
const THEME_MODE_DARK = 'noir';
const DEFAULT_THEME_MODE: ThemeMode = prefersDarkMode ? THEME_MODE_DARK : THEME_MODE_LIGHT;

export {
  DEFAULT_THEME_MODE,
  PREFERENCES_THEME_MODE,
  THEME_MODE_LIGHT,
  THEME_MODE_DARK,
};
