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
import PropTypes from 'prop-types';

import breakpoints, { breakpointPropTypes } from './breakpoints';
import colors, { colorsPropTypes } from './colors';
import fonts, { fontsPropTypes } from './fonts';
import utils, { utilsPropTypes } from './utils';
import spacings, { spacingsPropTypes } from './spacings';

const themePropTypes = PropTypes.shape({
  breakpoints: breakpointPropTypes,
  colors: colorsPropTypes,
  fonts: fontsPropTypes,
  utils: utilsPropTypes,
  spacings: spacingsPropTypes,
});

export {
  breakpoints,
  colors,
  fonts,
  utils,
  themePropTypes,
  spacings,
};
