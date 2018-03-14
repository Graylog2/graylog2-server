import PropTypes from 'prop-types';
import React from 'react';

class GracePeriodSummary extends React.Component {
  static propTypes = {
    alertCondition: PropTypes.object.isRequired,
  };

  _formatTime = (time) => {
    if (time === 1) {
      return '1 minute';
    }

    return `${time} minutes`;
  };

  render() {
    const time = this.props.alertCondition.parameters.grace;
    return <span>Grace period: {this._formatTime(time)}.</span>;
  }
}

export default GracePeriodSummary;
