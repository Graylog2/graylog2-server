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

export type Spacing = {
  0: string,
  1: string,
  xxs: string,
  xs: string,
  sm: string,
  md: string,
  lg: string,
  xl: string,
  xxl: string,
};

export const spacingPropTypes = PropTypes.shape({
  0: PropTypes.string,
  1: PropTypes.string,
  xxs: PropTypes.string,
  xs: PropTypes.string,
  sm: PropTypes.string,
  md: PropTypes.string,
  lg: PropTypes.string,
  xl: PropTypes.string,
  xxl: PropTypes.string,
});

const base = '1rem';

let count;
const fibSequence = [0, 1];
const sizes = ['xxs', 'xs', 'sm', 'md', 'lg', 'xl', 'xxl'];

for (count = 2; count <= 8; count += 1) {
  fibSequence[count] = fibSequence[count - 2] + fibSequence[count - 1];
}

const spacing = {
  0: '0',
  1: '1px',
} as Spacing;

sizes.forEach((size, index) => {
  spacing[size] = `calc(${0.25 * fibSequence[index + 2]} * ${base})`;
});

export default spacing;
