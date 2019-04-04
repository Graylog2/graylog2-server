import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Button, DropdownButton, MenuItem } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import { AlertConditionForm, AlertConditionSummary, AlertConditionTestModal, UnknownAlertCondition } from 'components/alertconditions';
import PermissionsMixin from 'util/PermissionsMixin';
import CombinedProvider from 'injection/CombinedProvider';
import Routes from 'routing/Routes';

const { AlertConditionsActions } = CombinedProvider.get('AlertConditions');
const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

const AlertCondition = createReactClass({
  displayName: 'AlertCondition',

  propTypes: {
    alertCondition: PropTypes.object.isRequired,
    conditionType: PropTypes.object.isRequired,
    stream: PropTypes.object.isRequired,
    isDetailsView: PropTypes.bool,
    isStreamView: PropTypes.bool,
    onUpdate: PropTypes.func,
    onDelete: PropTypes.func,
  },
  mixins: [Reflux.connect(CurrentUserStore), PermissionsMixin],

  getDefaultProps() {
    return {
      isDetailsView: false,
      isStreamView: false,
      onUpdate: () => {},
      onDelete: () => {},
    };
  },

  _onEdit() {
    this.updateForm.open();
  },

  _onUpdate(request) {
    AlertConditionsActions.update(this.props.stream.id, this.props.alertCondition.id, request)
      .then(() => this.props.onUpdate(this.props.stream.id, this.props.alertCondition.id));
  },

  _onDelete() {
    if (window.confirm('Really delete alert condition?')) {
      AlertConditionsActions.delete(this.props.stream.id, this.props.alertCondition.id)
        .then(() => this.props.onDelete(this.props.stream.id, this.props.alertCondition.id));
    }
  },

  _openTestModal() {
    this.modal.open();
  },

  render() {
    const { stream } = this.props;
    const condition = this.props.alertCondition;
    const { conditionType } = this.props;

    if (!conditionType) {
      return <UnknownAlertCondition alertCondition={condition} onDelete={this._onDelete} stream={stream} />;
    }

    const { permissions } = this.state.currentUser;
    let actions = [];
    if (this.isPermitted(permissions, `streams:edit:${stream.id}`)) {
      actions = [
        <Button key="test-button" bsStyle="info" onClick={this._openTestModal}>Test</Button>,
        <DropdownButton key="more-actions-button"
                        title="More actions"
                        pullRight
                        id={`more-actions-dropdown-${condition.id}`}>
          {!this.props.isStreamView && (
            <LinkContainer to={Routes.stream_alerts(stream.id)}>
              <MenuItem>Alerting overview for Stream</MenuItem>
            </LinkContainer>
          )}
          <MenuItem onSelect={this._onEdit}>Edit</MenuItem>
          <MenuItem divider />
          <MenuItem onSelect={this._onDelete}>Delete</MenuItem>
        </DropdownButton>,
      ];
    }

    return (
      <React.Fragment>
        <AlertConditionForm ref={(updateForm) => { this.updateForm = updateForm; }}
                            conditionType={conditionType}
                            alertCondition={condition}
                            onSubmit={this._onUpdate} />
        <AlertConditionTestModal ref={(c) => { this.modal = c; }}
                                 stream={stream}
                                 condition={condition} />
        <AlertConditionSummary alertCondition={condition}
                               conditionType={conditionType}
                               stream={stream}
                               actions={actions}
                               isDetailsView={this.props.isDetailsView} />
      </React.Fragment>
    );
  },
});

export default AlertCondition;
