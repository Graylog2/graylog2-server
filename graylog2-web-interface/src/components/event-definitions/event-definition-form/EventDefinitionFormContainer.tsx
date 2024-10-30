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
import { PluginStore } from 'graylog-web-plugin/plugin';
import cloneDeep from 'lodash/cloneDeep';

import { useStore } from 'stores/connect';
import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';
import { ConfirmLeaveDialog, Spinner } from 'components/common';
import { AvailableEventDefinitionTypesStore } from 'stores/event-definitions/AvailableEventDefinitionTypesStore';
import { ConfigurationsActions } from 'stores/configurations/ConfigurationsStore';
import { EventDefinitionsActions } from 'stores/event-definitions/EventDefinitionsStore';
import { EventNotificationsActions, EventNotificationsStore } from 'stores/event-notifications/EventNotificationsStore';
import type { EventDefinition, EventDefinitionFormControlsProps } from 'components/event-definitions/event-definitions-types';
import useCurrentUser from 'hooks/useCurrentUser';
import useEventDefinitionConfigFromLocalStorage from 'components/event-definitions/hooks/useEventDefinitionConfigFromLocalStorage';
import { getPathnameWithoutId } from 'util/URLUtils';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useScopePermissions from 'hooks/useScopePermissions';

import EventDefinitionForm, { STEP_KEYS } from './EventDefinitionForm';

const fetchNotifications = () => {
  EventNotificationsActions.listAll();
};

type Props = {
  action?: 'edit' | 'create'
  eventDefinition?: EventDefinition
  formControls?: React.ComponentType<EventDefinitionFormControlsProps>,
  initialStep?: string,
  onCancel?: () => void
  onChangeStep?: (step: string) => void,
  onEventDefinitionChange?: (nextEventDefinition: EventDefinition) => void
  onSubmit?: () => void,
}

const getConditionPlugin = (edType): any => {
  if (edType === undefined) {
    return {};
  }

  return PluginStore.exports('eventDefinitionTypes').find((eventDefinitionType) => eventDefinitionType.type === edType) || {};
};

const EventDefinitionFormContainer = ({
  action = 'edit',
  eventDefinition: eventDefinitionInitial = {
    title: '',
    description: '',
    priority: EventDefinitionPriorityEnum.NORMAL,
    // @ts-ignore
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
  formControls,
  initialStep = STEP_KEYS[0],
  onCancel,
  onChangeStep,
  onEventDefinitionChange = () => {},
  onSubmit,
}: Props) => {
  const [activeStep, setActiveStep] = useState(initialStep);
  const [eventDefinition, setEventDefinition] = useState(eventDefinitionInitial);
  const [validation, setValidation] = useState({ errors: {} });
  const [eventsClusterConfig, setEventsClusterConfig] = useState(undefined);
  const [isDirty, setIsDirty] = useState(false);
  const { configFromLocalStorage, hasLocalStorageConfig } = useEventDefinitionConfigFromLocalStorage();
  const { loadingScopePermissions, scopePermissions } = useScopePermissions(eventDefinition);

  const entityTypes = useStore(AvailableEventDefinitionTypesStore);
  const notifications = useStore(EventNotificationsStore);
  const currentUser = useCurrentUser();
  const { pathname } = useLocation();
  const sendTelemetry = useSendTelemetry();

  const isLoading = !entityTypes || !notifications.all || !eventsClusterConfig;
  const defaults = { default_backlog_size: eventsClusterConfig?.events_notification_default_backlog };

  const fetchClusterConfig = useCallback(() => {
    ConfigurationsActions.listEventsClusterConfig().then((config) => setEventsClusterConfig(config));
  }, []);

  const handleChange = useCallback((key: string, value: unknown) => {
    setEventDefinition((prev) => ({ ...prev, [key]: value }));
    onEventDefinitionChange({ ...eventDefinition, [key]: value });
    setIsDirty(true);
  }, [eventDefinition, onEventDefinitionChange, setEventDefinition, setIsDirty]);

  useEffect(() => {
    fetchClusterConfig();
    fetchNotifications();

    if (hasLocalStorageConfig) {
      const conditionPlugin = getConditionPlugin(configFromLocalStorage.type);
      const defaultConfig = conditionPlugin?.defaultConfig || {} as EventDefinition['config'];

      setEventDefinition((cur) => {
        const cloned = cloneDeep(cur);

        return ({
          ...cloned,
          config: {
            ...defaultConfig,
            ...cloned.config,
            ...configFromLocalStorage,
          },
        });
      });
    }
  }, [configFromLocalStorage, fetchClusterConfig, hasLocalStorageConfig]);

  const handleSubmitSuccessResponse = () => {
    setIsDirty(false);

    onSubmit();
  };

  const showValidationErrors = (errors: { errors: unknown }) => {
    setValidation(errors);
    setActiveStep(STEP_KEYS[STEP_KEYS.length - 1]);
  };

  const handleSubmitFailureResponse = (errorResponse) => {
    const { body } = errorResponse.additional;

    if (errorResponse.status === 400) {
      if (body && body.failed) {
        showValidationErrors(body);

        return;
      }

      if (body.type && body.type === 'ApiError') {
        if (body.message.includes('org.graylog.events.conditions.Expression')
          || body.message.includes('org.graylog.events.conditions.Expr')
          || body.message.includes('org.graylog.events.processor.aggregation.AggregationSeries')) {
          showValidationErrors({
            errors: { conditions: ['Aggregation condition is not valid'] },
          });

          return;
        }

        if (body.message.includes('embryonic')) {
          showValidationErrors({
            errors: { query_parameters: ['Query parameters must be declared'] },
          });
        }
      }
    }
  };

  const handleSubmit = () => {
    setIsDirty(false);

    if (action === 'create') {
      sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_SUMMARY.CREATE_CLICKED, {
        app_pathname: getPathnameWithoutId(pathname),
        app_section: 'new-event-definition',
        app_action_value: 'create-event-definition-button',
      });

      EventDefinitionsActions.create(eventDefinition)
        .then(handleSubmitSuccessResponse, handleSubmitFailureResponse);
    } else {
      sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_SUMMARY.UPDATE_CLICKED, {
        app_pathname: getPathnameWithoutId(pathname),
        app_section: 'edit-event-definition',
        app_action_value: 'update-event-definition-button',
      });

      EventDefinitionsActions.update(eventDefinition.id, eventDefinition)
        .then(handleSubmitSuccessResponse, handleSubmitFailureResponse);
    }
  };

  const handleCancel = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_SUMMARY.CANCEL_CLICKED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: (action === 'create') ? 'new-event-definition' : 'edit-event-definition',
      app_action_value: 'cancel-button',
    });

    onCancel();
  };

  const changeStep = (step: string) => {
    onChangeStep?.(step);
    setActiveStep(step);
  };

  if (isLoading || loadingScopePermissions) {
    return <Spinner text="Loading Event information..." />;
  }

  return (
    <>
      {isDirty && (
        <ConfirmLeaveDialog question="Do you really want to abandon this page and lose your changes? This action cannot be undone." />
      )}
      <EventDefinitionForm action={action}
                           canEdit={scopePermissions.is_mutable}
                           currentUser={currentUser}
                           defaults={defaults}
                           activeStep={activeStep}
                           entityTypes={entityTypes}
                           eventDefinition={eventDefinition}
                           formControls={formControls}
                           notifications={notifications.all}
                           onCancel={handleCancel}
                           onChange={handleChange}
                           onChangeStep={changeStep}
                           onSubmit={handleSubmit}
                           validation={validation} />
    </>
  );
};

export default EventDefinitionFormContainer;
