import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { PluginStore } from 'graylog-web-plugin/plugin';
import { ButtonToolbar, Col, ControlLabel, FormGroup, HelpBlock, Row, Button } from 'components/graylog';
import { Select, Spinner } from 'components/common';
import { Input } from 'components/bootstrap';

import FormsUtils from 'util/FormsUtils';


class EventNotificationForm extends React.Component {
  static propTypes = {
    action: PropTypes.oneOf(['create', 'edit']),
    notification: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    formId: PropTypes.string,
    embedded: PropTypes.bool.isRequired,
    onChange: PropTypes.func.isRequired,
    onCancel: PropTypes.func.isRequired,
    onSubmit: PropTypes.func.isRequired,
    onTest: PropTypes.func.isRequired,
  };

  static defaultProps = {
    action: 'edit',
    formId: undefined,
  };

  constructor(props, context) {
    super(props, context);
    this.state = { testRunning: false };
  }

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

  getNotificationPlugin = (type) => {
    if (type === undefined) {
      return {};
    }
    return PluginStore.exports('eventNotificationTypes').find(n => n.type === type) || {};
  };

  handleTypeChange = (nextType) => {
    const notificationPlugin = this.getNotificationPlugin(nextType);
    const defaultConfig = notificationPlugin.defaultConfig || {};
    this.handleConfigChange({ ...defaultConfig, type: nextType });
  };

  handleTestTrigger = (notification) => {
    this.setState({ testRunning: true });
    const { onTest } = this.props;
    onTest(notification).finally(() => this.setState({ testRunning: false }));
  };

  formattedEventNotificationTypes = () => {
    return PluginStore.exports('eventNotificationTypes')
      .map(type => ({ label: type.displayName, value: type.type }));
  };

  render() {
    const { action, embedded, formId, notification, onCancel, validation } = this.props;

    const notificationPlugin = this.getNotificationPlugin(notification.config.type);
    const notificationFormComponent = notificationPlugin.formComponent
      ? React.createElement(notificationPlugin.formComponent, {
        config: notification.config,
        onChange: this.handleConfigChange,
        validation: validation,
      })
      : null;

    const { testRunning } = this.state;
    const testButtonText = testRunning ? <Spinner text="Testing..." /> : 'Test';

    return (
      <Row>
        <Col md={12}>
          <form onSubmit={this.handleSubmit} id={formId}>
            <Input id="notification-title"
                   name="title"
                   label="Title"
                   type="text"
                   bsStyle={validation.errors.title ? 'error' : null}
                   help={lodash.get(validation, 'errors.title[0]', 'Title to identify this Notification.')}
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

            <FormGroup controlId="notification-type" validationState={validation.errors.config ? 'error' : null}>
              <ControlLabel>Notification Type</ControlLabel>
              <Select id="notification-type"
                      options={this.formattedEventNotificationTypes()}
                      value={notification.config.type}
                      onChange={this.handleTypeChange}
                      clearable={false}
                      required />
              <HelpBlock>
                {lodash.get(validation, 'errors.config[0]', 'Choose the type of Notification to create.')}
              </HelpBlock>
            </FormGroup>

            {notificationFormComponent}

            {!embedded && (
              <div>
                <Button bsStyle="info" disabled={testRunning} onClick={() => this.handleTestTrigger(notification)}> {testButtonText} </Button>
                <HelpBlock>
                  Trigger this notification with a test Alert
                </HelpBlock>
                <ButtonToolbar>
                  <Button bsStyle="primary" type="submit">{action === 'create' ? 'Create' : 'Update'}</Button>
                  <Button onClick={onCancel}>Cancel</Button>
                </ButtonToolbar>
              </div>
            )}
          </form>
        </Col>
      </Row>
    );
  }
}

export default EventNotificationForm;
