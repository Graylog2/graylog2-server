import React from 'react';
import Reflux from 'reflux';
import { Button, Col, DropdownButton, MenuItem } from 'react-bootstrap';

import PermissionsMixin from 'util/PermissionsMixin';

import CombinedProvider from 'injection/CombinedProvider';
const { AlertNotificationsStore } = CombinedProvider.get('AlertNotifications');
const { AlarmCallbacksActions } = CombinedProvider.get('AlarmCallbacks');
const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

import { EntityListItem, Spinner } from 'components/common';
import { UnknownAlertNotification } from 'components/alertnotifications';
import { ConfigurationForm, ConfigurationWell } from 'components/configurationforms';

const AlertNotification = React.createClass({
  propTypes: {
    alertNotification: React.PropTypes.object.isRequired,
    stream: React.PropTypes.object,
    onNotificationUpdate: React.PropTypes.func,
    onNotificationDelete: React.PropTypes.func,
  },
  mixins: [Reflux.connect(AlertNotificationsStore), Reflux.connect(CurrentUserStore), PermissionsMixin],

  getInitialState() {
    return {
      isTestingAlert: false,
    };
  },

  _onTestNotification() {
    this.setState({ isTestingAlert: true });
    AlertNotificationsStore.testAlert(this.props.alertNotification.id)
      .finally(() => this.setState({ isTestingAlert: false }));
  },

  _onEdit() {
    this.refs.configurationForm.open();
  },

  _onSubmit(data) {
    AlarmCallbacksActions.update(this.props.alertNotification.stream_id, this.props.alertNotification.id, data)
      .then(() => {
        if (typeof this.props.onNotificationUpdate === 'function') {
          this.props.onNotificationUpdate();
        }
      });
  },

  _onDelete() {
    if (window.confirm('Really delete alert notification?')) {
      AlarmCallbacksActions.delete(this.props.alertNotification.stream_id, this.props.alertNotification.id)
        .then(() => {
          if (typeof this.props.onNotificationUpdate === 'function') {
            this.props.onNotificationUpdate();
          }
        });
    }
  },

  render() {
    if (!this.state.availableNotifications) {
      return <Spinner />;
    }

    const notification = this.props.alertNotification;
    const stream = this.props.stream;
    const typeDefinition = this.state.availableNotifications[notification.type];

    if (!typeDefinition) {
      return <UnknownAlertNotification alertNotification={notification} onDelete={this._onDelete} />;
    }

    const description = (stream ?
      <span>Executed once per triggered alert condition in stream <em>{stream.title}</em></span>
      : 'Not executed, as it is not connected to a stream');

    const actions = this.isPermitted(this.state.currentUser.permissions, [`streams:edit:${stream.id}`]) && [
      <Button key="test-button" bsStyle="info" disabled={this.state.isTestingAlert} onClick={this._onTestNotification}>
        {this.state.isTestingAlert ? 'Testing...' : 'Test'}
      </Button>,
      <DropdownButton key="more-actions-button" title="More actions" pullRight
                      id={`more-actions-dropdown-${notification.id}`}>
        <MenuItem onSelect={this._onEdit}>Edit</MenuItem>
        <MenuItem divider />
        <MenuItem onSelect={this._onDelete}>Delete</MenuItem>
      </DropdownButton>,
    ];

    const content = (
      <Col md={12}>
        <div className="alert-callback alarm-callbacks">
          <ConfigurationForm ref="configurationForm"
                             key={`configuration-form-notification-${notification.id}`}
                             configFields={typeDefinition.requested_configuration}
                             title={'Editing alert configuration '}
                             typeName={notification.type}
                             titleValue={notification.title}
                             submitAction={this._onSubmit}
                             values={notification.configuration} />
          <ConfigurationWell configuration={notification.configuration} typeDefinition={typeDefinition} />
        </div>
      </Col>
    );

    return (
      <EntityListItem key={`entry-list-${notification.id}`}
                      title={notification.title ? notification.title : 'Untitled'}
                      titleSuffix={`(${typeDefinition.name})`}
                      description={description}
                      actions={actions}
                      contentRow={content} />
    );
  },
});

export default AlertNotification;
