import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import { Button } from 'react-bootstrap';
import { BootstrapModalForm } from 'components/bootstrap';
import { IfPermitted, ISODurationInput } from 'components/common';
import ObjectUtils from 'util/ObjectUtils';
import FormUtils from 'util/FormsUtils';
import Input from 'components/bootstrap/Input';

const EventsConfig = createReactClass({
  displayName: 'EventsConfig',

  propTypes: {
    config: PropTypes.shape({
      events_search_timeout: PropTypes.string,
    }),
    updateConfig: PropTypes.func.isRequired,
  },

  getDefaultProps() {
    return {
      config: {
        events_search_timeout: 'PT1M',
        events_notification_retry_period: 'PT5M',
        events_notification_default_backlog: 50,
      },
    };
  },

  getInitialState() {
    const { config } = this.props;
    return {
      config: ObjectUtils.clone(config),
    };
  },

  componentWillReceiveProps(newProps) {
    this.setState({ config: ObjectUtils.clone(newProps.config) });
  },

  _openModal() {
    this.modal.open();
  },

  _closeModal() {
    this.modal.close();
  },

  _resetConfig() {
    // Reset to initial state when the modal is closed without saving.
    this.setState(this.getInitialState());
  },

  _saveConfig() {
    const { updateConfig } = this.props;
    const { config } = this.state;
    updateConfig(config).then(() => {
      this._closeModal();
    });
  },

  _onUpdate(field) {
    return (value) => {
      const { config } = this.state;
      const update = ObjectUtils.clone(config);
      if (typeof value === 'object') {
        update[field] = FormUtils.getValueFromInput(value.target);
      } else {
        update[field] = value;
      }
      this.setState({ config: update });
    };
  },

  _searchTimeoutValidator(milliseconds) {
    return milliseconds >= 1000;
  },

  _notificationsRetryValidator(milliseconds) {
    return milliseconds >= 0;
  },

  render() {
    const { config } = this.state;
    const eventsSearchTimeout = config.events_search_timeout;
    const eventsNotificationRetryPeriod = config.events_notification_retry_period;
    const eventsNotificationDefaultBacklog = config.events_notification_default_backlog;
    return (
      <div>
        <h2>Events System</h2>

        <dl className="deflist">
          <dt>Search Timeout:</dt>
          <dd>{eventsSearchTimeout}</dd>
          <dt>Notification Retry:</dt>
          <dd>{eventsNotificationRetryPeriod}</dd>
          <dt>Notification Backlog:</dt>
          <dd>{eventsNotificationDefaultBacklog}</dd>
        </dl>

        <IfPermitted permissions="clusterconfigentry:edit">
          <Button bsStyle="info" bsSize="xs" onClick={this._openModal}>Update</Button>
        </IfPermitted>

        <BootstrapModalForm ref={(modal) => { this.modal = modal; }}
                            title="Update Events System Configuration"
                            onSubmitForm={this._saveConfig}
                            onModalClose={this._resetConfig}
                            submitButtonText="Save">
          <fieldset>
            <ISODurationInput id="search-timeout-field"
                              duration={eventsSearchTimeout}
                              update={this._onUpdate('events_search_timeout')}
                              label="Search Timeout (as ISO8601 Duration)"
                              help="Amount of time after which an Elasticsearch query is interrupted."
                              validator={this._searchTimeoutValidator}
                              errorText="invalid (min: 1 second)"
                              required />
            <ISODurationInput id="notifications-retry-field"
                              duration={eventsNotificationRetryPeriod}
                              update={this._onUpdate('events_notification_retry_period')}
                              label="Notifications retry period (as ISO8601 Duration)"
                              help="Amount of time after which a failed notification is resend."
                              validator={this._notificationsRetryValidator}
                              errorText="invalid (min: 0 second)"
                              required />
            <Input id="notification-backlog-field"
                   type="number"
                   onChange={this._onUpdate('events_notification_default_backlog')}
                   label="Default notifications backlog size"
                   help="Amount of log messages included in a notification by default."
                   value={eventsNotificationDefaultBacklog}
                   min="0"
                   required />
          </fieldset>
        </BootstrapModalForm>
      </div>
    );
  },
});

export default EventsConfig;
