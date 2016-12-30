import React from 'react';
import Reflux from 'reflux';
import { DropdownButton, MenuItem } from 'react-bootstrap';

import CombinedProvider from 'injection/CombinedProvider';
const { AlertConditionsActions, AlertConditionsStore } = CombinedProvider.get('AlertConditions');

import { AlertConditionSummary, UnknownAlertCondition } from 'components/alertconditions';

const AlertCondition = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object.isRequired,
    stream: React.PropTypes.object,
  },
  mixins: [Reflux.connect(AlertConditionsStore)],

  _onDelete() {
    if (window.confirm('Really delete alert condition?')) {
      AlertConditionsActions.delete(this.props.stream.id, this.props.alertCondition.id);
    }
  },

  render() {
    const type = this.props.alertCondition.type;
    const stream = this.props.stream;
    const condition = this.props.alertCondition;
    const typeDefinition = this.state.types[type];

    if (!typeDefinition) {
      return <UnknownAlertCondition alertCondition={condition} onDelete={this._onDelete} stream={stream} />;
    }

    const actions = [
      <DropdownButton key="more-actions-button" title="Actions" pullRight
                      id={`more-actions-dropdown-${condition.id}`}>
        <MenuItem onSelect={this._onDelete}>Delete</MenuItem>
      </DropdownButton>,
    ];

    return (
      <AlertConditionSummary alertCondition={condition} typeDefinition={typeDefinition} stream={stream}
                             actions={actions} linkToDetails />
    );
  },
});

export default AlertCondition;
