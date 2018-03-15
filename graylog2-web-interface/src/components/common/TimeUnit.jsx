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
    return (<span>
      {this.props.value}&nbsp;{this.UNITS[this.props.unit]}
    </span>);
  },
});

export default TimeUnit;
