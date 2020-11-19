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
import chroma from 'chroma-js';

export const lightThemeRatio = ['0.22', '0.55', '0.88'];
export const darkThemeRatio = ['0.15', '0.55', '0.95'];

function lighten(color, ratio) { return chroma.mix(color, '#fff', ratio).hex(); }
function darken(color, ratio) { return chroma.mix(color, '#000', ratio).hex(); }

export {
  darken,
  lighten,
};
