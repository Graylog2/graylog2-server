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
import colorLevel from './colorLevel';
import contrastingColor from './contrastingColor';
import opacify from './opacify';
import readableColor from './readableColor';
import mixColor from './mixColor';
import flattenColorStack from './flattenColorStack';

const utils = {
  colorLevel,
  contrastingColor,
  mixColor,
  opacify,
  readableColor,
  flattenColorStack,
};

export {
  colorLevel,
  contrastingColor,
  mixColor,
  opacify,
  readableColor,
  flattenColorStack,
};

export default utils;
