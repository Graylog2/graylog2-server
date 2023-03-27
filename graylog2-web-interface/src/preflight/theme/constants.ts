const prefersDarkMode = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;

export type ThemeMode = 'teint' | 'noir';
export type PreferencesThemeMode = 'themeMode';

const PREFERENCES_THEME_MODE: PreferencesThemeMode = 'themeMode';
const ROOT_FONT_SIZE = 14;
const THEME_MODE_LIGHT = 'teint';
const THEME_MODE_DARK = 'noir';
const DEFAULT_THEME_MODE: ThemeMode = prefersDarkMode ? THEME_MODE_DARK : THEME_MODE_LIGHT;
const THEME_MODES: Array<ThemeMode> = [THEME_MODE_LIGHT, THEME_MODE_DARK];

export {
  DEFAULT_THEME_MODE,
  PREFERENCES_THEME_MODE,
  ROOT_FONT_SIZE,
  THEME_MODE_LIGHT,
  THEME_MODE_DARK,
  THEME_MODES,
};
