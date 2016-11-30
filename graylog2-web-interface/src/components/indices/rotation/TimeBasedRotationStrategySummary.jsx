import React from 'react';

import moment from 'moment';
import {} from 'moment-duration-format';

const TimeBasedRotationStrategySummary = React.createClass({
  propTypes: {
    config: React.PropTypes.object.isRequired,
  },

  _humanizedPeriod() {
    const duration = moment.duration(this.props.config.rotation_period);

    return `${duration.format()}, ${duration.humanize()}`;
  },

  render() {
    return (
      <div>
        <dl>
          <dt>Index rotation strategy:</dt>
          <dd>Index Time</dd>
          <dt>Rotation period:</dt>
          <dd>{this.props.config.rotation_period} ({this._humanizedPeriod()})</dd>
        </dl>
      </div>
    );
  },
});

export default TimeBasedRotationStrategySummary;
