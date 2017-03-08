import React from 'react';

const BacklogSummary = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object.isRequired,
  },
  _formatMessageCount(count) {
    if (count === 0) {
      return 'Not including any messages';
    }

    if (count === 1) {
      return 'Including last message';
    }

    return `Including last ${count} messages`;
  },
  render() {
    const backlog = this.props.alertCondition.parameters.backlog;
    return (
      <span>{this._formatMessageCount(backlog)} in alert notification.</span>
    );
  },
});

export default BacklogSummary;
