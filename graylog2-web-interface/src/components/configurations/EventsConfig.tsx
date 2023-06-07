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
import * as React from 'react';
import { useEffect, useState } from 'react';//
import capitalize from 'lodash/capitalize';
import moment from 'moment';

import { useStore } from 'stores/connect';
import type { Store } from 'stores/StoreTypes';
import { ConfigurationsActions, ConfigurationsStore } from 'stores/configurations/ConfigurationsStore';
import { getConfig } from 'components/configurations/helpers';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import { Button, FormGroup, HelpBlock, BootstrapModalForm } from 'components/bootstrap';
import { IfPermitted, TimeUnitInput } from 'components/common';
import Spinner from 'components/common/Spinner';
import { getValueFromInput } from 'util/FormsUtils';
import Input from 'components/bootstrap/Input';
import { extractDurationAndUnit } from 'components/common/TimeUnitInput';

const TIME_UNITS = ['HOURS', 'MINUTES', 'SECONDS'];
const DEFAULT_CATCH_UP_WINDOW = 3600000;

type Config = {
  events_search_timeout: number,
  events_notification_retry_period: number,
  events_notification_default_backlog: number,
  events_catchup_window: number,
  events_notification_tcp_keepalive: boolean,
}

const DEFAULT_CONFIG = {
  events_search_timeout: 60000,
  events_notification_retry_period: 300000,
  events_notification_default_backlog: 50,
  events_catchup_window: DEFAULT_CATCH_UP_WINDOW,
  events_notification_tcp_keepalive: false,
};

const EventsConfig = () => {
  const [showConfigModal, setShowConfigModal] = useState(false);
  const [viewConfig, setViewConfig] = useState<Config>(DEFAULT_CONFIG);
  const [formConfig, setFormConfig] = useState<Config>(DEFAULT_CONFIG);
  const configuration = useStore(ConfigurationsStore as Store<Record<string, any>>, (state) => state?.configuration);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    ConfigurationsActions.list(ConfigurationType.EVENTS_CONFIG).then(() => {
      const config = getConfig(ConfigurationType.EVENTS_CONFIG, configuration);

      setViewConfig(config);
      setFormConfig(config);
      setLoaded(true);
    });
  }, [configuration]);

  const openModal = () => {
    setShowConfigModal(true);
  };

  const closeModal = () => {
    setShowConfigModal(false);
    setFormConfig(viewConfig);
  };

  const saveConfig = () => {
    ConfigurationsActions.update(ConfigurationType.EVENTS_CONFIG, formConfig).then(() => {
      closeModal();
    });
  };

  const propagateChanges = (key, value) => {
    setFormConfig({ ...formConfig, [key]: value });
  };

  const searchTimeoutValidator = (milliseconds) => milliseconds >= 1000;

  const notificationsRetryValidator = (milliseconds) => milliseconds >= 0;

  const onSearchTimeoutUpdate = (nextValue, nextUnit, enabled) => {
    const durationInMs = enabled ? moment.duration(nextValue, nextUnit).asMilliseconds() : 0;

    if (searchTimeoutValidator(durationInMs)) {
      propagateChanges('events_search_timeout', durationInMs);
    }
  };

  const onRetryPeriodUpdate = (nextValue, nextUnit, enabled) => {
    const durationInMs = enabled ? moment.duration(nextValue, nextUnit).asMilliseconds() : 0;

    if (notificationsRetryValidator(durationInMs)) {
      propagateChanges('events_notification_retry_period', durationInMs);
    }
  };

  const onBacklogUpdate = (event) => {
    const value = getValueFromInput(event.target);

    propagateChanges('events_notification_default_backlog', value);
  };

  const onCatchUpWindowUpdate = (nextValue, nextUnit, nextEnabled) => {
    if (formConfig.events_catchup_window === 0 && nextEnabled) {
      propagateChanges('events_catchup_window', DEFAULT_CATCH_UP_WINDOW);

      return;
    }

    const catchupWindowinMs = nextEnabled ? moment.duration(nextValue, nextUnit).asMilliseconds() : 0;

    propagateChanges('events_catchup_window', catchupWindowinMs);
  };

  const onNotificationTcpKeepAliveUpdate = (event) => {
    const value = getValueFromInput(event.target);
    propagateChanges('events_notification_tcp_keepalive', value);
  };

  const titleCase = (str) => capitalize(str);

  if (!loaded || !viewConfig) { return <Spinner />; }

  const eventsSearchTimeout = (config) => extractDurationAndUnit(config.events_search_timeout, TIME_UNITS);
  const eventsNotificationRetryPeriod = (config) => extractDurationAndUnit(config.events_notification_retry_period, TIME_UNITS);
  const eventsCatchupWindow = (config) => extractDurationAndUnit(config.events_catchup_window, TIME_UNITS);
  const eventsNotificationDefaultBacklog = (config) => config.events_notification_default_backlog;
  const eventsNotificationTcpKeepalive = (config) => config.events_notification_tcp_keepalive;

  return (
    <div>
      <h2>Events Configuration</h2>

      <dl className="deflist">
        <dt>Search Timeout:</dt>
        <dd>{eventsSearchTimeout(viewConfig).duration} {titleCase(eventsSearchTimeout(viewConfig).unit)}</dd>
        <dt>Notification Retry:</dt>
        <dd>{eventsNotificationRetryPeriod(viewConfig).duration} {titleCase(eventsNotificationRetryPeriod(viewConfig).unit)}</dd>
        <dt>Notification Backlog:</dt>
        <dd>{eventsNotificationDefaultBacklog(viewConfig)}</dd>
        <dt>Catch Up Window:</dt>
        <dd>{eventsCatchupWindow(viewConfig).duration > 0 ? eventsCatchupWindow(viewConfig).duration : 'disabled'} {eventsCatchupWindow(viewConfig).duration > 0 ? titleCase(eventsCatchupWindow(viewConfig).unit) : ''}</dd>
        <dt>TCP keep-alive probes:</dt>
        <dd>{eventsNotificationTcpKeepalive(viewConfig) ? 'enabled' : 'disabled'}</dd>
      </dl>

      <IfPermitted permissions="clusterconfigentry:edit">
        <Button bsStyle="info" bsSize="xs" onClick={openModal}>Edit configuration</Button>
      </IfPermitted>

      {showConfigModal && formConfig && (
      <BootstrapModalForm show
                          title="Update Events System Configuration"
                          onSubmitForm={saveConfig}
                          onCancel={closeModal}
                          submitButtonText="Update configuration">
        <fieldset>
          <FormGroup controlId="search-timeout-field">
            <TimeUnitInput label="Search Timeout"
                           update={onSearchTimeoutUpdate}
                           value={eventsSearchTimeout(formConfig).duration}
                           unit={eventsSearchTimeout(formConfig).unit}
                           units={TIME_UNITS}
                           required />
            <HelpBlock>
              Amount of time after which an Elasticsearch query is interrupted. (Minimum timeout is 1s)
            </HelpBlock>
          </FormGroup>
          <FormGroup controlId="notifications-retry-field">
            <TimeUnitInput label="Notifications retry period"
                           update={onRetryPeriodUpdate}
                           value={eventsNotificationRetryPeriod(formConfig).duration}
                           unit={eventsNotificationRetryPeriod(formConfig).unit}
                           units={TIME_UNITS}
                           required />
            <HelpBlock>
              Amount of time after which a failed notification is resend. (Minimum is 0 or immediate retry)
            </HelpBlock>
          </FormGroup>
          <Input id="notification-backlog-field"
                 type="number"
                 onChange={onBacklogUpdate}
                 label="Default notifications backlog size"
                 help="Amount of log messages included in a notification by default."
                 value={eventsNotificationDefaultBacklog(formConfig)}
                 min="0"
                 required />
          <FormGroup controlId="catch-up-window">
            <TimeUnitInput label="Catch up window size"
                           update={onCatchUpWindowUpdate}
                           value={eventsCatchupWindow(formConfig).duration}
                           unit={eventsCatchupWindow(formConfig).unit}
                           enabled={eventsCatchupWindow(formConfig).duration > 0}
                           units={TIME_UNITS} />
            <HelpBlock>If Event processor execution is behind schedule, queries on older data will be run with this window size to speed up processing.
              (If the &quot;search within the last&quot; setting of an event definition is greater, this setting will be ignored)
            </HelpBlock>
          </FormGroup>
          <FormGroup controlId="notification-tcp-keepalive-field">
            <Input id="notification-tcp-keepalive-field"
                   label="Send TCP keep-alive probes for notification connections"
                   type="checkbox"
                   onChange={onNotificationTcpKeepAliveUpdate}
                   checked={eventsNotificationTcpKeepalive(formConfig)} />
            <HelpBlock>
              If enabled, http connections for notifications will send TCP keep-alive probes
            </HelpBlock>
          </FormGroup>
        </fieldset>
      </BootstrapModalForm>
      )}
    </div>
  );
};

export default EventsConfig;
