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

type Props = {
  children: React.ReactElement[],
};

const stopPropagation = (evt) => {
  evt.stopPropagation();
  evt.preventDefault();
};

const StopPropagation = ({ children }: Props) => (
  <span role="presentation" onClick={stopPropagation} onMouseDown={stopPropagation}>
    {children}
  </span>
);

export default StopPropagation;
