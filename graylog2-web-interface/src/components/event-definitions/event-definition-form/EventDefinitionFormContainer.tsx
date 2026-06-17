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
import React, { useCallback, useState } from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';
import cloneDeep from 'lodash/cloneDeep';
import { useQueryClient } from '@tanstack/react-query';

import { getPathnameWithoutId } from 'util/URLUtils';
import { ConfirmLeaveDialog, Spinner } from 'components/common';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import type {
  EventDefinition,
  EventDefinitionFormControlsProps,
} from 'components/event-definitions/event-definitions-types';
import useCurrentUser from 'hooks/useCurrentUser';
import useEventDefinitionConfigFromLocalStorage from 'components/event-definitions/hooks/useEventDefinitionConfigFromLocalStorage';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useScopePermissions from 'hooks/useScopePermissions';
import type { EntitySharePayload } from 'actions/permissions/EntityShareActions';
import { useEventNotifications } from 'components/event-notifications/hooks/useEventNotifications';

import useEventDefinitionSteps, { INITIAL_EVENT_DEFINITION } from './useEventDefinitionCommonSteps';
import EventDetailsForm from './EventDetailsForm';
import EventConditionForm from './EventConditionForm';
import EventDefinitionForm, { getStepKeys } from './EventDefinitionForm';

import useEventDefinitionMutations from '../hooks/useEventDefinitionMutations';
import {
  updateEventDefinition,
  useGetEntityTypes,
  useGetListEventsClusterConfig,
  EVENT_DEFINITIONS_QUERY_KEY,
} from '../hooks/useEventDefinitions';

const STEP_KEYS = {
  EVENT_DETAILS: 'event-details',
  CONDITION: 'condition',
  SUMMARY: 'summary',
};

type Props = {
  action?: 'edit' | 'create';
  eventDefinition?: EventDefinition & {
    share_request?: EntitySharePayload;
  };
  formControls?: React.ComponentType<EventDefinitionFormControlsProps>;
  initialStep?: string;
  onCancel?: () => void;
  onChangeStep?: (step: string) => void;
  onEventDefinitionChange?: (nextEventDefinition: EventDefinition) => void;
  onSubmit?: () => void;
};

export const getConditionPlugin = (type: string | undefined) => {
  if (type === undefined) {
    return { displayName: null, hideFieldsStep: false, defaultConfig: {} };
  }

  return PluginStore.exports('eventDefinitionTypes').find((edt) => edt.type === type) || {};
};

const EventDefinitionFormContainer = ({
  action = 'edit',
  eventDefinition: eventDefinitionInitial = INITIAL_EVENT_DEFINITION,
  formControls = undefined,
  initialStep = 'event-details',
  onCancel = undefined,
  onChangeStep = undefined,
  onEventDefinitionChange = () => {},
  onSubmit = undefined,
}: Props) => {
  const [activeStep, setActiveStep] = useState(initialStep);
  const { configFromLocalStorage, hasLocalStorageConfig } = useEventDefinitionConfigFromLocalStorage();
  // Merge the optional localStorage-stored config into the initial event definition once, at
  // first render. Previously this happened in a useEffect that called setEventDefinition,
  // which trips the react-hooks/set-state-in-effect rule.
  const [eventDefinition, setEventDefinition] = useState<EventDefinition>(() => {
    if (!hasLocalStorageConfig) return eventDefinitionInitial;

    const localStorageConditionPlugin = getConditionPlugin(configFromLocalStorage.type);
    const defaultConfig = localStorageConditionPlugin?.defaultConfig || ({} as EventDefinition['config']);
    const cloned = cloneDeep(eventDefinitionInitial);

    return {
      ...cloned,
      config: {
        ...defaultConfig,
        ...cloned.config,
        ...configFromLocalStorage,
      },
    } as EventDefinition;
  });

  const [validation, setValidation] = useState({ errors: {} });
  const [isDirty, setIsDirty] = useState(false);
  const queryClient = useQueryClient();
  const { loadingScopePermissions, scopePermissions } = useScopePermissions(eventDefinition);
  const { createEventDefinition } = useEventDefinitionMutations();
  const { entityTypes, loadingEntityTypes } = useGetEntityTypes();
  const { eventsClusterConfig, loadingEventsClusterConfig } = useGetListEventsClusterConfig();
  const { data: eventNotificationsData, isLoading: loadingEventNotifications } = useEventNotifications();
  const notifications = eventNotificationsData?.notifications ?? [];
  const currentUser = useCurrentUser();
  const { pathname } = useLocation();
  const sendTelemetry = useSendTelemetry();
  const isNew = action === 'create';
  const eventDefinitionType = getConditionPlugin(eventDefinition.config.type);
  const hideFieldsStep = eventDefinitionType?.hideFieldsStep ?? false;
  const currentStepKeys = getStepKeys(isNew, hideFieldsStep);

  const isLoading = !entityTypes || loadingEntityTypes || loadingEventNotifications || loadingEventsClusterConfig;
  const defaults = {
    default_backlog_size: eventsClusterConfig?.events_notification_default_backlog as number,
  };

  const handleChange = useCallback(
    (key: string, value: unknown) => {
      setEventDefinition((prev) => ({ ...prev, [key]: value }));
      onEventDefinitionChange({ ...eventDefinition, [key]: value } as EventDefinition);
      setIsDirty(true);
      // Drop any stale submit-time validation error for the field being edited so the user
      // gets immediate feedback on their fix. Each field still owns its live client-side
      // validation; this only clears the server-returned error from the last submit attempt.
      setValidation((prev) => {
        if (!prev.errors || !(key in prev.errors)) return prev;
        const { [key]: _dropped, ...remaining } = prev.errors as Record<string, unknown>;

        return { ...prev, errors: remaining };
      });
    },
    [eventDefinition, onEventDefinitionChange, setEventDefinition, setIsDirty],
  );

  const commonStepProps = {
    key: eventDefinition.id, // Recreate components if ID changed
    action,
    entityTypes,
    eventDefinition,
    onChange: handleChange,
    validation,
    currentUser,
  };

  const canEdit = scopePermissions.is_mutable;
  const canEditCondition = canEdit || eventDefinition._scope.toUpperCase() === 'ILLUMINATE';

  const viewSteps = [
    {
      key: STEP_KEYS.EVENT_DETAILS,
      title: 'Event Details',
      component: (
        <EventDetailsForm
          {...commonStepProps}
          eventDefinitionEventProcedure={eventDefinition?.event_procedure}
          canEdit={scopePermissions.is_mutable}
        />
      ),
    },
    {
      key: STEP_KEYS.CONDITION,
      title: eventDefinitionType?.displayName ?? 'Condition',
      component: <EventConditionForm {...commonStepProps} canEdit={canEditCondition} />,
    },
  ];

  const steps = useEventDefinitionSteps({
    viewSteps,
    commonStepProps,
    notifications,
    notificationDefaults: defaults,
    canEdit,
  });

  const handleSubmitSuccessResponse = () => {
    setIsDirty(false);
    queryClient.invalidateQueries({ queryKey: EVENT_DEFINITIONS_QUERY_KEY });
    CurrentUserStore.update(currentUser.username);

    onSubmit();
  };

  const showValidationErrors = (errors: { errors: unknown }) => {
    setValidation(errors);
    setActiveStep(currentStepKeys[currentStepKeys.length - 1]);
  };

  const handleSubmitFailureResponse = (errorResponse: any) => {
    const { body } = errorResponse.additional;

    if (errorResponse.status === 400) {
      if (body && body.failed) {
        showValidationErrors(body);

        return;
      }

      if (body.type && body.type === 'ApiError') {
        if (
          body.message.includes('org.graylog.events.conditions.Expression') ||
          body.message.includes('org.graylog.events.conditions.Expr') ||
          body.message.includes('org.graylog.events.processor.aggregation.AggregationSeries')
        ) {
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

    const tacticsTechniquesTelemetry = {
      tactics_techniques_count: eventDefinition.tactics_techniques?.length ?? 0,
    };

    if (action === 'create') {
      sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_SUMMARY.CREATE_CLICKED, {
        app_pathname: getPathnameWithoutId(pathname),
        app_section: 'new-event-definition',
        app_action_value: 'create-event-definition-button',
        ...tacticsTechniquesTelemetry,
      });

      createEventDefinition(eventDefinition).then(handleSubmitSuccessResponse, handleSubmitFailureResponse);
    } else {
      sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_SUMMARY.UPDATE_CLICKED, {
        app_pathname: getPathnameWithoutId(pathname),
        app_section: 'edit-event-definition',
        app_action_value: 'update-event-definition-button',
        ...tacticsTechniquesTelemetry,
      });

      updateEventDefinition(eventDefinition.id, eventDefinition).then(
        handleSubmitSuccessResponse,
        handleSubmitFailureResponse,
      );
    }
  };

  const handleCancel = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_SUMMARY.CANCEL_CLICKED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: action === 'create' ? 'new-event-definition' : 'edit-event-definition',
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
      <EventDefinitionForm
        steps={steps}
        action={action}
        activeStep={activeStep}
        formControls={formControls}
        onCancel={handleCancel}
        onChangeStep={changeStep}
        onSubmit={handleSubmit}
      />
    </>
  );
};

export default EventDefinitionFormContainer;
