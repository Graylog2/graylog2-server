'use strict';

var React = require('react/addons');
var Alert = require('./Alert');

var AlertsTable = React.createClass({
    render() {
        if (this.props.alerts.length > 0) {
            var alerts = this.props.alerts.map((alert) => {
                return <Alert key={alert.id} alert={alert}/>;
            });
            return (
                <table className="alerts table table-hover table-condensed">
                    <thead>
                    <tr>
                        <th style={{width: "130px"}} data-dynatable-sorts="timestamp">Triggered</th>
                        <th style={{display: "none"}}>Timestamp</th>
                        <th>Condition</th>
                        <th>Description</th>
                    </tr>
                    </thead>
                    <tbody>
                        {alerts}
                    </tbody>
                </table>
            );
        } else {
            return (
                <div style={{marginTop: '10px'}} className="alert alert-info">
                    This stream has never triggered an alert.
                </div>
            );
        }
    }
});

module.exports = AlertsTable;
