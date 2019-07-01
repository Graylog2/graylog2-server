import React from 'react';
import PropTypes from 'prop-types';
import { Button, ButtonToolbar, Col, ControlLabel, FormGroup, HelpBlock, Row } from 'react-bootstrap';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { Select } from 'components/common';
import { Input } from 'components/bootstrap';

import FormsUtils from 'util/FormsUtils';

class EventDefinitionForm extends React.Component {
  static propTypes = {
    action: PropTypes.oneOf(['create', 'edit']),
    notification: PropTypes.object.isRequired,
    entityTypes: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    onCancel: PropTypes.func.isRequired,
    onSubmit: PropTypes.func.isRequired,
  };

  static defaultProps = {
    action: 'edit',
  };

  handleSubmit = (event) => {
    const { notification, onSubmit } = this.props;
    event.preventDefault();

    onSubmit(notification);
  };

  handleChange = (event) => {
    const { name } = event.target;
    const { onChange } = this.props;
    onChange(name, FormsUtils.getValueFromInput(event.target));
  };

  handleConfigChange = (nextConfig) => {
    const { onChange } = this.props;
    onChange('config', nextConfig);
  };

  handleTypeChange = (nextType) => {
    this.handleConfigChange({ type: nextType });
  };

  getNotificationPlugin = (type) => {
    if (type === undefined) {
      return {};
    }
    return PluginStore.exports('eventNotificationTypes').find(n => n.type === type);
  };

  formattedEventNotificationTypes = () => {
    return PluginStore.exports('eventNotificationTypes')
      .map(type => ({ label: type.displayName, value: type.type }));
  };

  render() {
    const { notification, onCancel } = this.props;

    const notificationPlugin = this.getNotificationPlugin(notification.config.type);
    const notificationFormComponent = notificationPlugin.formComponent
      ? React.createElement(notificationPlugin.formComponent, {
        config: notification.config,
        onChange: this.handleConfigChange
      })
      : null;

    return (
      <Row>
        <Col md={12}>
          <form onSubmit={this.handleSubmit}>
            <Input id="notification-title"
                   name="title"
                   label="Title"
                   type="text"
                   help="Title for this Notification."
                   value={notification.title}
                   onChange={this.handleChange}
                   required />

            <Input id="notification-description"
                   name="description"
                   label={<span>Description <small className="text-muted">(Optional)</small></span>}
                   type="textarea"
                   help="Longer description for this Notification."
                   value={notification.description}
                   onChange={this.handleChange}
                   rows={2} />

            <FormGroup controlId="notification-type">
              <ControlLabel>Notification Type</ControlLabel>
              <Select id="notification-type"
                      options={this.formattedEventNotificationTypes()}
                      value={notification.type}
                      onChange={this.handleTypeChange}
                      clearable={false}
                      required />
              <HelpBlock>Choose the type of Notification to create.</HelpBlock>
            </FormGroup>

            {notificationFormComponent}

            <ButtonToolbar>
              <Button bsStyle="primary" type="submit">Create</Button>
              <Button onClick={onCancel}>Cancel</Button>
            </ButtonToolbar>
          </form>
        </Col>
      </Row>
    );
  }
}

export default EventDefinitionForm;
