import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { DropdownButton, MenuItem } from 'react-bootstrap';

import { AlertConditionSummary, UnknownAlertCondition } from 'components/alertconditions';
import PermissionsMixin from 'util/PermissionsMixin';
import CombinedProvider from 'injection/CombinedProvider';

const { AlertConditionsActions, AlertConditionsStore } = CombinedProvider.get('AlertConditions');
const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

const AlertCondition = createReactClass({
  displayName: 'AlertCondition',

  propTypes: {
    alertCondition: PropTypes.object.isRequired,
    stream: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(AlertConditionsStore), Reflux.connect(CurrentUserStore), PermissionsMixin],

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

    const permissions = this.state.currentUser.permissions;
    let actions = [];
    if (this.isPermitted(permissions, `streams:edit:${stream.id}`)) {
      actions = [
        <DropdownButton key="more-actions-button"
                        title="Actions"
                        pullRight
                        id={`more-actions-dropdown-${condition.id}`}>
          <MenuItem onSelect={this._onDelete}>Delete</MenuItem>
        </DropdownButton>,
      ];
    }

    return (
      <AlertConditionSummary alertCondition={condition} typeDefinition={typeDefinition} stream={stream}
                             actions={actions} linkToDetails />
    );
  },
});

export default AlertCondition;
