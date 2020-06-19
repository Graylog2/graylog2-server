import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { EntityList } from 'components/common';
import { AlertCondition } from 'components/alertconditions';
import PermissionsMixin from 'util/PermissionsMixin';
import CombinedProvider from 'injection/CombinedProvider';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

const EditAlertConditionForm = createReactClass({
  displayName: 'EditAlertConditionForm',

  propTypes: {
    alertCondition: PropTypes.object.isRequired,
    conditionType: PropTypes.object.isRequired,
    stream: PropTypes.object.isRequired,
    onUpdate: PropTypes.func,
    onDelete: PropTypes.func,
  },

  mixins: [Reflux.connect(CurrentUserStore), PermissionsMixin],

  getDefaultProps() {
    return {
      onUpdate: () => {},
      onDelete: () => {},
    };
  },

  render() {
    const { alertCondition, conditionType, stream } = this.props;

    return (
      <div>
        <h2>Condition details</h2>
        <p>Define the condition to evaluate when triggering a new alert.</p>
        <EntityList items={[
          <AlertCondition key={alertCondition.id}
                          stream={stream}
                          alertCondition={alertCondition}
                          conditionType={conditionType}
                          onUpdate={this.props.onUpdate}
                          onDelete={this.props.onDelete}
                          isDetailsView />,
        ]} />
      </div>
    );
  },
});

export default EditAlertConditionForm;
