import React from 'react';

const GracePeriodSummary = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object.isRequired,
  },
  _formatTime(time) {
    if (time === 1) {
      return '1 minute';
    }

    return `${time} minutes`;
  },
  render() {
    const time = this.props.alertCondition.parameters.grace;
    return <span>Grace period: {this._formatTime(time)}.</span>;
  },
});

export default GracePeriodSummary;
