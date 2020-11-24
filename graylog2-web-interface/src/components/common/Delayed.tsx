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
import * as React from 'react';
import { useEffect, useState } from 'react';
import PropTypes from 'prop-types';

type Props = {
  children: Array<React.ReactElement | string> | React.ReactElement,
  delay: number,
};

const Delayed = ({ children, delay }: Props) => {
  const [delayFinished, setDelayFinished] = useState(delay <= 0);

  useEffect(() => {
    if (delay <= 0) {
      return () => {};
    }

    const delayTimeout = window.setTimeout(() => setDelayFinished(true), delay);

    return () => clearTimeout(delayTimeout);
  }, []);

  return delayFinished ? <>{children}</> : null;
};

Delayed.propTypes = {
  children: PropTypes.node.isRequired,
  delay: PropTypes.number.isRequired,
};

export default Delayed;
