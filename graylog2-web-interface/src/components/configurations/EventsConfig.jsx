import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import lodash from 'lodash';
import moment from 'moment';

import { Button, FormGroup, HelpBlock } from 'components/graylog';
import { BootstrapModalForm } from 'components/bootstrap';
import { IfPermitted, TimeUnitInput } from 'components/common';
import FormUtils from 'util/FormsUtils';
import Input from 'components/bootstrap/Input';
import { extractDurationAndUnit } from 'components/common/TimeUnitInput';

const TIME_UNITS = ['HOURS', 'MINUTES', 'SECONDS'];

const EventsConfig = createReactClass({
  displayName: 'EventsConfig',

  propTypes: {
    config: PropTypes.shape({
      events_search_timeout: PropTypes.number,
      events_notification_retry_period: PropTypes.number,
      events_notification_default_backlog: PropTypes.number,
    }),
    updateConfig: PropTypes.func.isRequired,
  },

  getDefaultProps() {
    return {
      config: {
        events_search_timeout: 60000,
        events_notification_retry_period: 300000,
        events_notification_default_backlog: 50,
      },
    };
  },

  getInitialState() {
    const { config } = this.props;
    return {
      config: config,
    };
  },

  componentWillReceiveProps(newProps) {
    this.setState({ config: newProps.config });
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

  _propagateChanges(key, value) {
    const { config } = this.state;
    const nextConfig = lodash.cloneDeep(config);
    nextConfig[key] = value;
    this.setState({ config: nextConfig });
  },

  _onSearchTimeoutUpdate(nextValue, nextUnit, enabled) {
    const durationInMs = enabled ? moment.duration(nextValue, nextUnit).asMilliseconds() : 0;
    if (this._searchTimeoutValidator(durationInMs)) {
      this._propagateChanges('events_search_timeout', durationInMs);
    }
  },

  _onRetryPeriodUpdate(nextValue, nextUnit, enabled) {
    const durationInMs = enabled ? moment.duration(nextValue, nextUnit).asMilliseconds() : 0;
    if (this._notificationsRetryValidator(durationInMs)) {
      this._propagateChanges('events_notification_retry_period', durationInMs);
    }
  },

  _searchTimeoutValidator(milliseconds) {
    return milliseconds >= 1000;
  },

  _notificationsRetryValidator(milliseconds) {
    return milliseconds >= 0;
  },

  _onBacklogUpdate(event) {
    const value = FormUtils.getValueFromInput(event.target);
    this._propagateChanges('events_notification_default_backlog', value);
  },

  _titleCase(str) {
    return lodash.capitalize(str);
  },

  render() {
    const { config } = this.state;
    const eventsSearchTimeout = extractDurationAndUnit(config.events_search_timeout, TIME_UNITS);
    const eventsNotificationRetryPeriod = extractDurationAndUnit(config.events_notification_retry_period, TIME_UNITS);
    const eventsNotificationDefaultBacklog = config.events_notification_default_backlog;
    return (
      <div>
        <h2>Events System</h2>

        <dl className="deflist">
          <dt>Search Timeout:</dt>
          <dd>{eventsSearchTimeout.duration} {this._titleCase(eventsSearchTimeout.unit)}</dd>
          <dt>Notification Retry:</dt>
          <dd>{eventsNotificationRetryPeriod.duration} {this._titleCase(eventsNotificationRetryPeriod.unit)}</dd>
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
            <FormGroup controlId="search-timeout-field">
              <TimeUnitInput label="Search Timeout"
                             update={this._onSearchTimeoutUpdate}
                             value={eventsSearchTimeout.duration}
                             unit={eventsSearchTimeout.unit}
                             units={TIME_UNITS}
                             required />
              <HelpBlock>
                Amount of time after which an Elasticsearch query is interrupted. (Minimum timeout is 1s)
              </HelpBlock>
            </FormGroup>
            <FormGroup controlId="notifications-retry-field">
              <TimeUnitInput label="Notifications retry period"
                             update={this._onRetryPeriodUpdate}
                             value={eventsNotificationRetryPeriod.duration}
                             unit={eventsNotificationRetryPeriod.unit}
                             units={TIME_UNITS}
                             required />
              <HelpBlock>
                Amount of time after which a failed notification is resend. (Minimum is 0 or immediate retry)
              </HelpBlock>
            </FormGroup>
            <Input id="notification-backlog-field"
                   type="number"
                   onChange={this._onBacklogUpdate}
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
