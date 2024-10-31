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
import { COLOR_SCHEME_DARK, COLOR_SCHEME_LIGHT } from '@graylog/sawmill';
import type { ColorScheme } from '@graylog/sawmill';

const prefersDarkMode = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;

export type LegacyColorScheme = 'teint' | 'noir';
export type PreferencesThemeMode = 'themeMode';

const PREFERENCES_THEME_MODE: PreferencesThemeMode = 'themeMode';
const ROOT_FONT_SIZE = 16; // This value is also being maintained as @font-size-base in bootstrap-config.js

const LEGACY_COLOR_SCHEME_LIGHT = 'teint';
const LEGACY_COLOR_SCHEME_DARK = 'noir';
const DEFAULT_THEME_MODE: ColorScheme = prefersDarkMode ? COLOR_SCHEME_DARK : COLOR_SCHEME_LIGHT;
const LEGACY_DEFAULT_THEME_MODE: LegacyColorScheme = prefersDarkMode ? LEGACY_COLOR_SCHEME_DARK : LEGACY_COLOR_SCHEME_LIGHT;
const COLOR_SCHEMES: Array<ColorScheme> = [COLOR_SCHEME_LIGHT, COLOR_SCHEME_DARK];
const INPUT_BORDER_RADIUS = 0;
const NAV_ITEM_HEIGHT = '50px';

export {
  DEFAULT_THEME_MODE,
  LEGACY_DEFAULT_THEME_MODE,
  LEGACY_COLOR_SCHEME_DARK,
  LEGACY_COLOR_SCHEME_LIGHT,
  PREFERENCES_THEME_MODE,
  ROOT_FONT_SIZE,
  COLOR_SCHEME_LIGHT,
  COLOR_SCHEME_DARK,
  COLOR_SCHEMES,
  INPUT_BORDER_RADIUS,
  NAV_ITEM_HEIGHT,
};
