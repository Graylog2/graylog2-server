import React, { PropTypes } from 'react';

import DateTime from 'logic/datetimes/DateTime';

const Timestamp = React.createClass({
  propTypes: {
    dateTime: PropTypes.oneOfType([PropTypes.string, PropTypes.object]).isRequired,
    format: PropTypes.string,
    relative: PropTypes.bool,
    tz: PropTypes.string,
  },
  getDefaultProps() {
    return {
      format: DateTime.Formats.TIMESTAMP,
    };
  },
  _formatDateTime() {
    const dateTime = new DateTime(this.props.dateTime);
    if (this.props.relative) {
      return dateTime.toRelativeString();
    }
    switch (this.props.tz) {
      case null:
      case undefined:
        return dateTime.toString(this.props.format);
      case 'browser':
        return dateTime.toBrowserLocalTime().toString(this.props.format);
      default:
        return dateTime.toTimeZone(this.props.tz).toString(this.props.format);

    }
  },
  render() {
    return (
      <time key={`time-${this.props.dateTime}`} dateTime={this.props.dateTime} title={this.props.dateTime}>
        {this._formatDateTime()}
      </time>
    );
  },
});

export default Timestamp;
