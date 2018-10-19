import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { EntityList, Spinner } from 'components/common';
import { AlertCondition } from 'components/alertconditions';
import PermissionsMixin from 'util/PermissionsMixin';

import CombinedProvider from 'injection/CombinedProvider';

const { AlertConditionsStore } = CombinedProvider.get('AlertConditions');
const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

const EditAlertConditionForm = createReactClass({
  displayName: 'EditAlertConditionForm',

  propTypes: {
    alertCondition: PropTypes.object.isRequired,
    stream: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(AlertConditionsStore), Reflux.connect(CurrentUserStore), PermissionsMixin],

  _isLoading() {
    return !this.state.types;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const { alertCondition, stream } = this.props;

    return (
      <div>
        <h2>Condition details</h2>
        <p>Define the condition to evaluate when triggering a new alert.</p>
        <EntityList items={[<AlertCondition stream={stream} alertCondition={alertCondition} />]} />
      </div>
    );
  },
});

export default EditAlertConditionForm;
