import PropTypes from 'prop-types';
import React from 'react';

import DateTime from 'logic/datetimes/DateTime';

/**
 * Component that renders a `time` HTML element with a given date time. It is
 * capable of render date times in different formats, accepting ISO 8601
 * strings, JS native Date objects, and Moment.js Date objects.
 *
 * The component can display the date time in different formats, and also can
 * show the relative time from/until now.
 *
 * It is also possible to change the time zone for the given date, something
 * that helps, for instance, to display a local time from a UTC time that
 * was used in the server.
 *
 */
class Timestamp extends React.Component {
  static propTypes = {
    /**
     * Date time to be displayed in the component. You can provide an ISO
     * 8601 string, a JS native `Date` object, or a moment `Date` object.
     */
    dateTime: PropTypes.oneOfType([PropTypes.string, PropTypes.object]).isRequired,
    /**
     * Format to use to represent the date time. It supports any format
     * supported by momentjs http://momentjs.com/docs/#/displaying/format/.
     * We also provide a list of default formats in 'logic/datetimes/DateTime':
     *
     *  - DATE: `YYYY-MM-DD`
     *  - DATETIME: `YYYY-MM-DD HH:mm:ss`, local times when decimal second precision is not important
     *  - DATETIME_TZ: `YYYY-MM-DD HH:mm:ss Z`, when decimal second precision is not important, but TZ is
     *  - TIMESTAMP: `YYYY-MM-DD HH:mm:ss.SSS`, local times when decimal second precision is important (e.g. search results)
     *  - TIMESTAMP_TZ: `YYYY-MM-DD HH:mm:ss.SSS Z`, when decimal second precision is important, in a different TZ
     *  - COMPLETE: `dddd D MMMM YYYY, HH:mm ZZ`, easy to read date time
     *  - ISO_8601: `YYYY-MM-DDTHH:mm:ss.SSSZ`
     */
    format: PropTypes.string,
    /** Specifies if the component should display relative time or not. */
    relative: PropTypes.bool,
    /**
     * Specifies the timezone to convert `dateTime`. Use `browser` to
     * convert the date time to the browser's local time, or one of the
     * time zones supported by moment timezone.
     */
    tz: PropTypes.string,
  };

  static defaultProps = {
    format: DateTime.Formats.TIMESTAMP,
  };

  _formatDateTime = () => {
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
  };

  render() {
    return (
      <time key={`time-${this.props.dateTime}`} dateTime={this.props.dateTime} title={this.props.dateTime}>
        {this._formatDateTime()}
      </time>
    );
  }
}

export default Timestamp;
