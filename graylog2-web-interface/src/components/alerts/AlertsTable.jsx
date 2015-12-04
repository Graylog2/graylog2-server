import React from 'react';

import Alert from 'components/alerts/Alert';

const AlertsTable = React.createClass({
  propTypes: {
    alerts: React.PropTypes.array.isRequired,
  },
  render() {
    if (this.props.alerts.length > 0) {
      return (
        <table className="alerts table table-hover table-condensed">
          <thead>
          <tr>
            <th style={{width: 150}}>Triggered</th>
            <th>Condition</th>
            <th>Reason</th>
            <th style={{width: 120}}>&nbsp;</th>
          </tr>
          </thead>
          {this.props.alerts.map((alert) => <Alert key={alert.id} alert={alert}/>)}
        </table>
      );
    } else {
      return (
        <div style={{marginTop: 10}} className="alert alert-info">
          This stream has never triggered an alert.
        </div>
      );
    }
  },
});

export default AlertsTable;
