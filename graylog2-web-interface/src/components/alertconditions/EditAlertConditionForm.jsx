import React from 'react';
import Reflux from 'reflux';
import { Button } from 'react-bootstrap';

import { EntityList, Spinner } from 'components/common';
import { AlertConditionForm, AlertConditionSummary } from 'components/alertconditions';

import CombinedProvider from 'injection/CombinedProvider';
const { AlertConditionsActions, AlertConditionsStore } = CombinedProvider.get('AlertConditions');

const EditAlertConditionForm = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object.isRequired,
    stream: React.PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(AlertConditionsStore)],

  _onEdit() {
    this.refs.updateForm.open();
  },

  _onUpdate(request) {
    AlertConditionsActions.update(this.props.stream.id, this.props.alertCondition.id, request).then(() => {
      AlertConditionsActions.get(this.props.stream.id, this.props.alertCondition.id);
    });
  },

  _formatCondition() {
    const type = this.props.alertCondition.type;
    const stream = this.props.stream;
    const condition = this.props.alertCondition;
    const typeDefinition = this.state.types[type];

    const actions = [
      <Button key="edit-button" bsStyle="info" onClick={this._onEdit}>Edit</Button>,
    ];

    return [
      <AlertConditionSummary key={`alert-condition-${condition.id}`} alertCondition={condition}
                             typeDefinition={typeDefinition} stream={stream}
                             actions={actions} />,
    ];
  },

  _isLoading() {
    return !this.state.types;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const condition = this.props.alertCondition;

    return (
      <div>
        <h2>Condition details</h2>
        <p>Define the condition to evaluate when triggering a new alert.</p>
        <AlertConditionForm ref="updateForm"
                            type={condition.type}
                            alertCondition={condition}
                            onSubmit={this._onUpdate} />
        <EntityList items={this._formatCondition()} />
      </div>
    );
  },
});

export default EditAlertConditionForm;
