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
import { MantineColors } from '../../mantine/types';
import { VARIANT_NAMES } from '../../THEME_BASE';
import { opacify } from '../../utils';
import { AlertColors } from '../types';

const alertColors = (colors: MantineColors, isLightTheme: boolean) => Object.fromEntries(VARIANT_NAMES.map((variantName) => [
  variantName,
  {
    background: isLightTheme ? opacify(colors[variantName][5], 0.1) : opacify(colors[variantName][3], 0.15),
    border: isLightTheme ? colors[variantName][2] : colors[variantName][8],
  },
])) as AlertColors;

export default alertColors;
