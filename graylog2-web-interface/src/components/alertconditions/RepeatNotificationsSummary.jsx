import React from 'react';

const RepeatNotificationsSummary = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object.isRequired,
  },
  render() {
    const backlog = this.props.alertCondition.parameters.repeat_notifications || false;
    return (
      <span>Configured to {!backlog && <b>not</b>} repeat notifications.</span>
    );
  },
});

export default RepeatNotificationsSummary;
