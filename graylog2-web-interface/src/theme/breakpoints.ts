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
// @flow strict
import PropTypes from 'prop-types';

const sizes: { [key: string]: number } = {
  xs: 480,
  sm: 768,
  md: 992,
  lg: 1200,
};

const min: { [key: string]: string } = {};
const max: { [key: string]: string } = {};

Object.keys(sizes).forEach((bp) => {
  min[bp] = `${sizes[bp]}px`;
  max[bp] = `${sizes[bp] - 1}px`;
});

type Breakpoint = {
  xs: string;
  sm: string;
  md: string;
  lg: string;
};

export type Breakpoints = {
  min: {
    xs: string,
    sm: string,
    md: string,
    lg: string,
  },
  max: {
    xs: string,
    sm: string,
    md: string,
    lg: string,
  },
};

export const breakpointPropTypes = PropTypes.shape({
  min: PropTypes.shape({
    xs: PropTypes.string,
    sm: PropTypes.string,
    md: PropTypes.string,
    lg: PropTypes.string,
  }),
  max: PropTypes.shape({
    xs: PropTypes.string,
    sm: PropTypes.string,
    md: PropTypes.string,
    lg: PropTypes.string,
  }),
});

const breakpoints: Breakpoints = {
  min: min as Breakpoint,
  max: max as Breakpoint,
};

export default breakpoints;
