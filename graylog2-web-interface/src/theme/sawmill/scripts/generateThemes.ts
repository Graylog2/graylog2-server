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
import path from 'path';

import buildMantineTheme from './mantine/buildMantineTheme';
import buildStyledComponentsTheme from './styled-components/buildStyledComponentsTheme';
import writeThemeFile from './writeThemeFile';

const mantineTheme = buildMantineTheme();
writeThemeFile(path.resolve(__dirname, '../mantine/generated/theme.json'), mantineTheme);
const styledComponentsTheme = buildStyledComponentsTheme();
writeThemeFile(path.resolve(__dirname, '../styled-components/generated/theme.json'), styledComponentsTheme);
