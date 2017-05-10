import React, { PropTypes } from 'react';

const TimeUnit = React.createClass({
  propTypes: {
    value: PropTypes.number.isRequired,
    unit: PropTypes.oneOf(['NANOSECONDS', 'MICROSECONDS', 'MILLISECONDS', 'SECONDS', 'MINUTES', 'HOURS', 'DAYS']).isRequired,
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
