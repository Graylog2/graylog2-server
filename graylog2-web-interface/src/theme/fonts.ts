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

import { ROOT_FONT_SIZE } from './constants';

import '@openfonts/source-code-pro_latin/index.css';
import '@openfonts/source-sans-pro_latin/index.css';
import '@openfonts/barlow-condensed_latin/index.css';

export type Fonts = {
  family: {
    body: string,
    monospace: string,
  },
  size: {
    root: string,
    body: string,
    huge: string,
    large: string,
    small: string,
    tiny: string,
    h1: string,
    h2: string,
    h3: string,
    h4: string,
    h5: string,
    h6: string,
  },
};

export const fontsPropTypes = PropTypes.shape({
  family: PropTypes.shape({
    body: PropTypes.string,
    monospace: PropTypes.string,
  }),
  size: PropTypes.shape({
    root: PropTypes.string,
    body: PropTypes.string,
    huge: PropTypes.string,
    large: PropTypes.string,
    small: PropTypes.string,
    tiny: PropTypes.string,
    h1: PropTypes.string,
    h2: PropTypes.string,
    h3: PropTypes.string,
    h4: PropTypes.string,
    h5: PropTypes.string,
    h6: PropTypes.string,
  }),
});

const family = {
  body: '"Source Sans Pro", "Helvetica Neue", Helvetica, Arial, sans-serif',
  monospace: '"Source Code Pro", Menlo, Monaco, Consolas, "Courier New", monospace',
  navigation: '"Barlow Condensed", "Helvetica Neue", Helvetica, Arial, sans-serif',
};

/* Scaled 1.067 Minor Second - https://type-scale.com/ */
const size = {
  root: `${ROOT_FONT_SIZE}px`,
  body: '1rem',
  huge: '1.383rem',
  large: '1.067rem',
  small: '0.889rem',
  tiny: '0.823rem',
  navigation: '1.067rem',
  h1: '1.575rem',
  h2: '1.296rem',
  h3: '1.215rem',
  h4: '1.138rem',
  h5: '1.067rem',
  h6: '1rem',
};

const fonts: Fonts = {
  family,
  size,
};

export default fonts;
