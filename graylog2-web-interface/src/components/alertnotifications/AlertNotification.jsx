import React from 'react';
import Reflux from 'reflux';
import { Button, Col, DropdownButton, MenuItem } from 'react-bootstrap';

import CombinedProvider from 'injection/CombinedProvider';
const { AlertNotificationsStore, AlertNotificationsActions } = CombinedProvider.get('AlertNotifications');
const { AlarmCallbacksActions } = CombinedProvider.get('AlarmCallbacks');
const { StreamsStore } = CombinedProvider.get('Streams');

import { EntityListItem } from 'components/common';
import { UnknownAlertNotification } from 'components/alertnotifications';
import { ConfigurationForm, ConfigurationWell } from 'components/configurationforms';

const AlertNotification = React.createClass({
  propTypes: {
    alertNotification: React.PropTypes.object.isRequired,
    stream: React.PropTypes.object,
  },
  mixins: [Reflux.connect(AlertNotificationsStore)],

  _onTestNotification() {
    StreamsStore.sendDummyAlert(this.props.alertNotification.stream_id);
  },

  _onEdit() {
    this.refs.configurationForm.open();
  },

  _onSubmit(data) {
    AlarmCallbacksActions.update(this.props.alertNotification.stream_id, this.props.alertNotification.id, data)
      .then(() => AlertNotificationsActions.listAll());
  },

  _onDelete() {
    if (window.confirm('Really delete alert notification?')) {
      AlarmCallbacksActions.delete(this.props.alertNotification.stream_id, this.props.alertNotification.id)
        .then(() => AlertNotificationsActions.listAll());
    }
  },

  render() {
    const notification = this.props.alertNotification;
    const stream = this.props.stream;
    const typeDefinition = this.state.availableNotifications[notification.type];

    if (!typeDefinition) {
      return <UnknownAlertNotification alertNotification={notification} onDelete={this._onDelete} />;
    }

    const description = (stream ?
      <span>Executed once per triggered alert condition in stream <em>{stream.title}</em></span>
      : 'Not executed, as it is not connected to a stream');

    const actions = [
      <Button key="test-button" bsStyle="info" onClick={this._onTestNotification}>Test</Button>,
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
                             title={"Editing alert configuration "}
                             typeName={notification.type}
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
