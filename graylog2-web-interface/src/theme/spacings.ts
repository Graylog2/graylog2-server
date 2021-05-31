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

const SPACE = 0.08; // ratio we use for our calculations
const SIZES = ['0', '1', 'xxs', 'xs', 'sm', 'md', 'lg', 'xl', 'xxl'] as const;
const FIBONACCI = [0, 1, 3, 5, 8, 13, 21, 34, 55, 89, 144]; // skipped [1, 2]

type Sizes = typeof SIZES[number]
export type Spacings = Record<Sizes, string> & { px: Record<Sizes, number> };

const props = SIZES.reduce((acc, key) => ({ ...acc, [key]: PropTypes.string }), {
  px: PropTypes.shape(SIZES.reduce((acc, key) => ({ ...acc, [key]: PropTypes.number }), {})),
});

export const spacingsPropTypes = PropTypes.shape(props);

const spacings = {
  px: {},
} as Spacings;

SIZES.forEach((size, index) => {
  if (size === '0') {
    spacings[size] = '0px';
    spacings.px[size] = 0;
  } else if (size === '1') {
    spacings[size] = '1px';
    spacings.px[size] = 1;
  } else {
    const value = SPACE * FIBONACCI[index];

    spacings[size] = `${value.toFixed(2)}rem`;
    spacings.px[size] = Math.round(value * ROOT_FONT_SIZE);
  }
});

export default spacings;
