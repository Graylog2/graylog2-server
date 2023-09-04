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
import type { ColorScheme } from '@graylog/sawmill';
import { COLOR_SCHEME_LIGHT, COLOR_SCHEME_DARK } from '@graylog/sawmill';

const prefersDarkMode = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;

const DEFAULT_THEME_MODE: ColorScheme = prefersDarkMode ? COLOR_SCHEME_DARK : COLOR_SCHEME_LIGHT;

// eslint-disable-next-line import/prefer-default-export
export { DEFAULT_THEME_MODE };
