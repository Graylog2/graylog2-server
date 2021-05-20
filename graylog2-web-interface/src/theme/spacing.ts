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
  base: string,
  none: string,
  px: string,
  xxs: string,
  xs: string,
  sm: string,
  md: string,
  lg: string,
  xl: string,
  xxl: string,
};

export const spacingPropTypes = PropTypes.shape({
  base: PropTypes.string,
  none: PropTypes.string,
  px: PropTypes.string,
  xxs: PropTypes.string,
  xs: PropTypes.string,
  sm: PropTypes.string,
  md: PropTypes.string,
  lg: PropTypes.string,
  xl: PropTypes.string,
  xxl: PropTypes.string,
});

const base = '1rem';

const spacing = {
  base,
  none: '0',
  px: '1px',
  xxs: `calc(0.25 * ${base})`,
  xs: `calc(0.5 * ${base})`,
  sm: `calc(0.75 * ${base})`,
  md: `calc(1.25 * ${base})`,
  lg: `calc(2 * ${base})`,
  xl: `calc(3.25 * ${base})`,
  xxl: `calc(5.25 * ${base})`,
} as Spacing;

export default spacing;
