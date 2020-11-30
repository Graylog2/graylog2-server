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
import * as React from 'react';
import PropTypes from 'prop-types';

import { Alert } from 'components/graylog';

type Props = {
  children: React.ReactNode,
  className: string | null | undefined,
};

const EmptyResult = ({ children, className }: Props) => (
  <Alert className={`${className ?? ''} no-bm`}>{children}</Alert>
);

EmptyResult.propTypes = {
  children: PropTypes.oneOfType([PropTypes.string, PropTypes.element]),
  className: PropTypes.string,
};

EmptyResult.defaultProps = {
  children: 'No data available.',
  className: undefined,
};

export default EmptyResult;
