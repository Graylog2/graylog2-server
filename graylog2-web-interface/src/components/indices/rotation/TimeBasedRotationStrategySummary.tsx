import React from 'react';
import moment from 'moment';
import 'moment-duration-format';

type TimeBasedRotationStrategySummaryProps = {
  config: any;
};

class TimeBasedRotationStrategySummary extends React.Component<TimeBasedRotationStrategySummaryProps, {
  [key: string]: any;
}> {
  _humanizedPeriod = () => {
    const duration = moment.duration(this.props.config.rotation_period);

    return `${duration.format()}, ${duration.humanize()}`;
  };

  render() {
    return (
      <div>
        <dl>
          <dt>Index rotation strategy:</dt>
          <dd>Index Time</dd>
          <dt>Rotation period:</dt>
          <dd>{this.props.config.rotation_period} ({this._humanizedPeriod()})</dd>
          <dt>Rotate empty index set:</dt>
          <dd>{this.props.config.rotate_empty_index_set ? 'Yes' : 'No'}</dd>
        </dl>
      </div>
    );
  }
}

export default TimeBasedRotationStrategySummary;
