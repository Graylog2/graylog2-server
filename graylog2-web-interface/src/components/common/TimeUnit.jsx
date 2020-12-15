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
import React from 'react';
import createReactClass from 'create-react-class';

/**
 * Component that renders a time value given in a certain unit.
 * It can also use 0 as never if `zeroIsNever` is set.
 */
const TimeUnit = createReactClass({
  displayName: 'TimeUnit',

  propTypes: {
    /** Value to display. */
    value: PropTypes.number.isRequired,
    /** Unit used in the value. */
    unit: PropTypes.oneOf(['NANOSECONDS', 'MICROSECONDS', 'MILLISECONDS', 'SECONDS', 'MINUTES', 'HOURS', 'DAYS']).isRequired,
    /** Specifies if zero should be displayed as never or not. */
    zeroIsNever: PropTypes.bool,
  },

  getDefaultProps() {
    return {
      zeroIsNever: true,
    };
  },

  UNITS: {
    NANOSECONDS: 'nanoseconds',
    MICROSECONDS: 'microseconds',
    MILLISECONDS: 'milliseconds',
    SECONDS: 'seconds',
    MINUTES: 'minutes',
    HOURS: 'hours',
    DAYS: 'days',
  },

  render() {
    if (this.props.value === 0 && this.props.zeroIsNever) {
      return <span>Never</span>;
    }

    return (
      <span>
        {this.props.value}&nbsp;{this.UNITS[this.props.unit]}
      </span>
    );
  },
});

export default TimeUnit;
