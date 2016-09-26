import React from 'react';

import AlertCondition from 'components/alertconditions/AlertCondition';

const AlertConditionsList = React.createClass({
  propTypes: {
    alertConditions: React.PropTypes.array.isRequired,
  },
  render() {
    if (this.props.alertConditions.length === 0) {
      return (
        <div
          style={{ marginTop: 10 }}
          className="alert alert-info">
          No configured alarm conditions.
        </div>
      );
    }

    return (
      <ul style={{ padding: 0 }}>
        {this.props.alertConditions.map((alertCondition) => {
          return (
            <li key={`alertCondition-${alertCondition.id}`} className="alert-condition-item">
              <AlertCondition alertCondition={alertCondition}/>
            </li>
          );
        })}
      </ul>
    );
  },
});

export default AlertConditionsList;
