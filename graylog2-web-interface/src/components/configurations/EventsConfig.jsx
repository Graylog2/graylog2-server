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
    return {
      config: ObjectUtils.clone(this.props.config),
    };
  },

  componentWillReceiveProps(newProps) {
    this.setState({ config: ObjectUtils.clone(newProps.config) });
  },

  _openModal() {
    this.refs.configModal.open();
  },

  _closeModal() {
    this.refs.configModal.close();
  },

  _resetConfig() {
    // Reset to initial state when the modal is closed without saving.
    this.setState(this.getInitialState());
  },

  _saveConfig() {
    this.props.updateConfig(this.state.config).then(() => {
      this._closeModal();
    });
  },

  _onUpdate(field) {
    return (value) => {
      const update = ObjectUtils.clone(this.state.config);
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
    return (
      <div>
        <h2>Events System</h2>

        <dl className="deflist">
          <dt>Search Timeout:</dt>
          <dd>{this.state.config.events_search_timeout}</dd>
          <dt>Notification Retry:</dt>
          <dd>{this.state.config.events_notification_retry_period}</dd>
          <dt>Notification Backlog:</dt>
          <dd>{this.state.config.events_notification_default_backlog}</dd>
        </dl>

        <IfPermitted permissions="clusterconfigentry:edit">
          <Button bsStyle="info" bsSize="xs" onClick={this._openModal}>Update</Button>
        </IfPermitted>

        <BootstrapModalForm ref="configModal"
                            title="Update Events System Configuration"
                            onSubmitForm={this._saveConfig}
                            onModalClose={this._resetConfig}
                            submitButtonText="Save">
          <fieldset>
            <ISODurationInput id="search-timeout-field"
                              duration={this.state.config.events_search_timeout}
                              update={this._onUpdate('events_search_timeout')}
                              label="Search Timeout (as ISO8601 Duration)"
                              help="Amount of time after which an Elasticsearch query is interrupted."
                              validator={this._searchTimeoutValidator}
                              errorText="invalid (min: 1 second)"
                              required />
            <ISODurationInput id="notifications-retry-field"
                              duration={this.state.config.events_notification_retry_period}
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
                   value={this.state.config.events_notification_default_backlog}
                   min="0"
                   required />
          </fieldset>
        </BootstrapModalForm>
      </div>
    );
  },
});

export default EventsConfig;
