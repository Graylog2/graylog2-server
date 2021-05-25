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

export type Spacings = {
  root: number,
  row: number,
  pageContent: number,
};

export const spacingsPropTypes = PropTypes.shape({
  root: PropTypes.number,
  row: PropTypes.number,
  pageContent: PropTypes.number,
});

const ROOT_MARGIN = 3;

const spacings: Spacings = {
  root: ROOT_MARGIN,
  pageContent: ROOT_MARGIN * 3,
  row: ROOT_MARGIN * 2,
};

export default spacings;
