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
import React, { useCallback, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import cloneDeep from 'lodash/cloneDeep';
import { useNavigate } from 'react-router-dom';
import { PluginStore } from 'graylog-web-plugin/plugin';

import Routes from 'routing/Routes';
import { useStore } from 'stores/connect';
import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';
import { ConfirmLeaveDialog, Spinner } from 'components/common';
import { AvailableEventDefinitionTypesStore } from 'stores/event-definitions/AvailableEventDefinitionTypesStore';
import { ConfigurationsActions } from 'stores/configurations/ConfigurationsStore';
import { EventDefinitionsActions } from 'stores/event-definitions/EventDefinitionsStore';
import { EventNotificationsActions, EventNotificationsStore } from 'stores/event-notifications/EventNotificationsStore';
import 'components/event-notifications/event-notification-types';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';
import useCurrentUser from 'hooks/useCurrentUser';
import useEventDefinitionConfigFromUrl from 'components/event-definitions/hooks/useEventDefinitionConfigFromUrl';

import EventDefinitionForm from './EventDefinitionForm';

const fetchNotifications = () => {
  EventNotificationsActions.listAll();
};

type Props = {
  action: 'edit' | 'create',
  eventDefinition: EventDefinition,
  onEventDefinitionChange: (nextEventDefinition: EventDefinition) => void,
}

const EventDefinitionFormContainer = ({ action, eventDefinition: eventDefinitionInitial, onEventDefinitionChange }: Props) => {
  const [eventDefinition, setEventDefinition] = useState(eventDefinitionInitial);
  const [validation, setValidation] = useState({ errors: {} });
  const [eventsClusterConfig, setEventsClusterConfig] = useState(undefined);
  const [isDirty, setIsDirty] = useState(false);
  const { configFromUrl, hasUrlConfig } = useEventDefinitionConfigFromUrl();

  const entityTypes = useStore(AvailableEventDefinitionTypesStore);
  const notifications = useStore(EventNotificationsStore);
  const currentUser = useCurrentUser();
  const navigate = useNavigate();

  const isLoading = !entityTypes || !notifications.all || !eventsClusterConfig;
  const defaults = { default_backlog_size: eventsClusterConfig?.events_notification_default_backlog };

  const fetchClusterConfig = useCallback(() => {
    ConfigurationsActions.listEventsClusterConfig().then((config) => setEventsClusterConfig(config));
  }, []);

  const handleChange = useCallback((key: string, value: unknown) => {
    setEventDefinition((curState) => {
      const nextEventDefinition = cloneDeep(curState);
      nextEventDefinition[key] = value;
      onEventDefinitionChange(nextEventDefinition);
      setIsDirty(true);

      return nextEventDefinition;
    });
  }, [onEventDefinitionChange]);

  const getConditionPlugin = (edType): any => {
    if (edType === undefined) {
      return {};
    }

    return PluginStore.exports('eventDefinitionTypes').find((eventDefinitionType) => eventDefinitionType.type === edType) || {};
  };

  useEffect(() => {
    fetchClusterConfig();
    fetchNotifications();

    if (hasUrlConfig) {
      const conditionPlugin = getConditionPlugin(configFromUrl.type);
      const defaultConfig = conditionPlugin?.defaultConfig || {} as EventDefinition['config'];

      setEventDefinition((cur) => {
        const cloned = cloneDeep(cur);

        return ({
          ...cloned,
          config: {
            ...defaultConfig,
            ...cloned.config,
            ...configFromUrl,
          },
        });
      });
    }
  }, [configFromUrl, fetchClusterConfig, hasUrlConfig]);

  const handleSubmitSuccessResponse = () => {
    setIsDirty(false);
    navigate(Routes.ALERTS.DEFINITIONS.LIST);
  };

  const handleSubmitFailureResponse = (errorResponse) => {
    const { body } = errorResponse.additional;

    if (errorResponse.status === 400) {
      if (body && body.failed) {
        setValidation(body);

        return;
      }

      if (body.type && body.type === 'ApiError') {
        if (body.message.includes('org.graylog.events.conditions.Expression')
          || body.message.includes('org.graylog.events.conditions.Expr')
          || body.message.includes('org.graylog.events.processor.aggregation.AggregationSeries')) {
          setValidation({
            errors: { conditions: ['Aggregation condition is not valid'] },
          });

          return;
        }

        if (body.message.includes('embryonic')) {
          setValidation({
            errors: { query_parameters: ['Query parameters must be declared'] },
          });
        }
      }
    }
  };

  const handleSubmit = () => {
    if (action === 'create') {
      EventDefinitionsActions.create(eventDefinition)
        .then(handleSubmitSuccessResponse, handleSubmitFailureResponse);
    } else {
      EventDefinitionsActions.update(eventDefinition.id, eventDefinition)
        .then(handleSubmitSuccessResponse, handleSubmitFailureResponse);
    }
  };

  const handleCancel = () => {
    navigate(Routes.ALERTS.DEFINITIONS.LIST);
  };

  if (isLoading) {
    return <Spinner text="Loading Event information..." />;
  }

  return (
    <>
      {isDirty && (
        <ConfirmLeaveDialog question="Do you really want to abandon this page and lose your changes? This action cannot be undone." />
      )}
      <EventDefinitionForm action={action}
                           eventDefinition={eventDefinition}
                           currentUser={currentUser}
                           validation={validation}
                           entityTypes={entityTypes}
                           notifications={notifications.all}
                           defaults={defaults}
                           onChange={handleChange}
                           onCancel={handleCancel}
                           onSubmit={handleSubmit} />
    </>
  );
};

EventDefinitionFormContainer.propTypes = {
  action: PropTypes.oneOf(['create', 'edit']),
  eventDefinition: PropTypes.object,
  onEventDefinitionChange: PropTypes.func,
};

EventDefinitionFormContainer.defaultProps = {
  action: 'edit',
  eventDefinition: {
    title: '',
    description: '',
    priority: EventDefinitionPriorityEnum.NORMAL,
    config: {},
    field_spec: {},
    key_spec: [],
    notification_settings: {
      grace_period_ms: 300000,
      // Defaults to system setting for notification backlog size
      backlog_size: null,
    },
    notifications: [],
    alert: false,
  },
  onEventDefinitionChange: () => {},
};

export default EventDefinitionFormContainer;
