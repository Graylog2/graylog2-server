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
import React from "react";
import { PluginStore } from "graylog-web-plugin/plugin";

import { TELEMETRY_EVENT_TYPE } from "logic/telemetry/Constants";
import EventDefinitionPriorityEnum from "logic/alerts/EventDefinitionPriorityEnum";
import type User from "logic/users/User";
import type { EventDefinition } from "components/event-definitions/event-definitions-types";
import type { EntitySharePayload } from "actions/permissions/EntityShareActions";
import type { EventNotification } from "stores/event-notifications/EventNotificationsStore";
import type { StepType } from "components/common/Wizard";

import FieldsForm from "./FieldsForm";
import ShareForm from "./ShareForm";
import NotificationsForm from "./NotificationsForm";
import EventDefinitionSummary from "./EventDefinitionSummary";

const COMMON_STEP_KEYS = {
  FIELDS: "fields",
  NOTIFICATIONS: "notifications",
  SHARE: "Share",
  SUMMARY: "summary",
};

export const COMMON_STEP_TELEMETRY_KEYS = [
  TELEMETRY_EVENT_TYPE.EVENTDEFINITION_FIELDS.STEP_CLICKED,
  TELEMETRY_EVENT_TYPE.EVENTDEFINITION_NOTIFICATIONS.STEP_CLICKED,
  TELEMETRY_EVENT_TYPE.EVENTDEFINITION_SUMMARY.STEP_CLICKED,
];

export const INITIAL_EVENT_DEFINITION: EventDefinition = {
  title: "",
  description: "",
  priority: EventDefinitionPriorityEnum.MEDIUM,
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
};

const getConditionPlugin = (edType): any => {
  if (edType === undefined) return {};

  return (
    PluginStore.exports("eventDefinitionTypes").find(
      (eventDefinitionType) => eventDefinitionType.type === edType,
    ) || {}
  );
};

type Args = {
  viewSteps: Array<StepType<string>>;
  commonStepProps: {
    key: string;
    action?: "edit" | "create";
    entityTypes: {};
    eventDefinition: EventDefinition & {
      share_request?: EntitySharePayload;
    };
    onChange: (key: string, value: unknown) => void;
    validation: {
      errors: {
        config?: unknown;
        title?: string;
      };
    };
    currentUser: User;
  };
  notifications: Array<EventNotification>;
  notificationDefaults: { default_backlog_size: number };
  canEdit: boolean;
};

function useEventDefinitionSteps({
  viewSteps,
  commonStepProps,
  notifications,
  notificationDefaults,
  canEdit,
}: Args): Array<StepType<string>> {
  const isNew = commonStepProps.action === "create";
  const conditionPlugin = getConditionPlugin(
    commonStepProps?.eventDefinition?.config?.type,
  );
  const hideFieldsStep = conditionPlugin?.hideFieldsStep ?? false;

  return [
    ...viewSteps,
    {
      key: COMMON_STEP_KEYS.FIELDS,
      title: "Fields",
      component: <FieldsForm {...commonStepProps} canEdit={canEdit} />,
      hidden: hideFieldsStep,
    },
    {
      key: COMMON_STEP_KEYS.NOTIFICATIONS,
      title: "Notifications",
      component: (
        <NotificationsForm
          {...commonStepProps}
          notifications={notifications}
          defaults={notificationDefaults}
        />
      ),
    },
    {
      key: COMMON_STEP_KEYS.SHARE,
      title: "Share",
      component: <ShareForm {...commonStepProps} />,
      hidden: !isNew,
    },
    {
      key: COMMON_STEP_KEYS.SUMMARY,
      title: "Summary",
      component: (
        <EventDefinitionSummary
          eventDefinition={commonStepProps.eventDefinition}
          currentUser={commonStepProps.currentUser}
          notifications={notifications}
          validation={commonStepProps.validation}
        />
      ),
    },
  ].filter((step) => !step.hidden);
}

export default useEventDefinitionSteps;
