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
          style={{marginTop: 10}}
          className="alert alert-info">
          No configured alarm conditions.
        </div>
      );
    }

    return (
      <span>
        {this.props.alertConditions.map((alertCondition) => <AlertCondition key={'alertCondition-' + alertCondition.id}
                                                                            alertCondition={alertCondition} />)}
      </span>
    );
  },
});

export default AlertConditionsList;
