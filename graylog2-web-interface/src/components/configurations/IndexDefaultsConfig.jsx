/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import lodash from 'lodash';
import moment from 'moment';

import { BootstrapModalForm, Button, FormGroup, HelpBlock } from 'components/bootstrap';
import { IfPermitted } from 'components/common';
import FormUtils from 'util/FormsUtils';
import Input from 'components/bootstrap/Input';
import { extractDurationAndUnit } from 'components/common/TimeUnitInput';

const IndexDefaultsConfig = createReactClass({
  displayName: 'IndexDefaultsConfig',

  propTypes: {
    config: PropTypes.shape({
      events_search_timeout: PropTypes.number,
      events_notification_retry_period: PropTypes.number,
      events_notification_default_backlog: PropTypes.number,
      events_catchup_window: PropTypes.number,
    }),
    updateConfig: PropTypes.func.isRequired,
  },

  getDefaultProps() {
    return {
      config: {
        events_search_timeout: 60000,
        events_notification_retry_period: 300000,
        events_notification_default_backlog: 50,
        events_catchup_window: DEFAULT_CATCH_UP_WINDOW,
      },
    };
  },

  getInitialState() {
    const { config } = this.props;

    return {
      config: config,
    };
  },

  UNSAFE_componentWillReceiveProps(newProps) {
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

  _onCatchUpWindowUpdate(nextValue, nextUnit, nextEnabled) {
    const { config } = this.state;

    if (config.events_catchup_window === 0 && nextEnabled) {
      this._propagateChanges('events_catchup_window', DEFAULT_CATCH_UP_WINDOW);

      return;
    }

    const catchupWindowinMs = nextEnabled ? moment.duration(nextValue, nextUnit).asMilliseconds() : 0;

    this._propagateChanges('events_catchup_window', catchupWindowinMs);
  },

  _titleCase(str) {
    return lodash.capitalize(str);
  },

  render() {
    const { config } = this.state;
    const eventsSearchTimeout = extractDurationAndUnit(config.events_search_timeout, TIME_UNITS);
    const eventsNotificationRetryPeriod = extractDurationAndUnit(config.events_notification_retry_period, TIME_UNITS);
    const eventsCatchupWindow = extractDurationAndUnit(config.events_catchup_window, TIME_UNITS);
    const eventsNotificationDefaultBacklog = config.events_notification_default_backlog;

    return (
      <div>
        <h2>Index Defaults</h2>
        <p>These defaults apply to new indices only.</p>
        <dl className="deflist">
          <dt>Shards per Index:</dt>
          <dd>{eventsNotificationDefaultBacklog}</dd>
          <dt>Replicas per Index:</dt>
          <dd>{eventsNotificationDefaultBacklog}</dd>
        </dl>

        <p>
          <IfPermitted permissions="clusterconfigentry:edit">
            <Button bsStyle="info" bsSize="xs" onClick={this._openModal}>Update</Button>
          </IfPermitted>
        </p>
        <BootstrapModalForm ref={(modal) => {
          this.modal = modal;
        }}
                            title="Update Events System Configuration"
                            onSubmitForm={this._saveConfig}
                            onModalClose={this._resetConfig}
                            submitButtonText="Save">
          <fieldset>
            <Input id="notification-backlog-field"
                   type="number"
                   onChange={this._onBacklogUpdate}
                   label="Shards per Index"
                   help="The default number of shards to specify for each new index."
                   value={eventsNotificationDefaultBacklog}
                   min="0"
                   required />
            <Input id="notification-backlog-field"
                   type="number"
                   onChange={this._onBacklogUpdate}
                   label="Replicas per Index"
                   help="The default number of shards to specify for each new index."
                   value={eventsNotificationDefaultBacklog}
                   min="0"
                   required />
          </fieldset>
        </BootstrapModalForm>
      </div>
    );
  },
});

export default IndexDefaultsConfig;
