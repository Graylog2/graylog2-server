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
import type { PluginExports } from 'graylog-web-plugin/plugin';

import Routes from 'routing/Routes';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

const entityCreatorBindings: PluginExports = {
  'entityCreators': [
    {
      id: 'Event Definition',
      title: 'Create event definition',
      path: Routes.ALERTS.DEFINITIONS.CREATE,
      telemetryEvent: {
        type: TELEMETRY_EVENT_TYPE.EVENTDEFINITION_CREATE_BUTTON_CLICKED,
        section: 'event-definitions',
        actionValue: 'create-event-definition-button',
      },
      permissions: 'eventdefinitions:create',
    },
    {
      id: 'Event Notification',
      title: 'Create notification',
      path: Routes.ALERTS.NOTIFICATIONS.CREATE,
      permissions: 'eventnotifications:create',
    },
    {
      id: 'Dashboard',
      title: 'Create dashboard',
      path: Routes.DASHBOARD.NEW,
      telemetryEvent: {
        type: TELEMETRY_EVENT_TYPE.DASHBOARD_ACTION.DASHBOARD_CREATE_CLICKED,
        section: 'dashboard',
        actionValue: 'dashboard-create-button',
      },
      permissions: 'dashboards:create',
    },
    {
      id: 'Stream',
      title: 'Create stream',
      path: Routes.STREAM_NEW,
      telemetryEvent: {
        type: TELEMETRY_EVENT_TYPE.STREAMS.CREATE_FORM_MODAL_OPENED,
        section: 'streams',
        actionValue: 'create-stream-button',
      },
      permissions: 'streams:create',
    },
    {
      id: 'Index Set',
      title: 'Create index set',
      path: Routes.SYSTEM.INDEX_SETS.CREATE,
      permissions: 'indexsets:create',
    },
    {
      id: 'User',
      title: 'Create user',
      path: Routes.SYSTEM.USERS.CREATE,
      permissions: 'users:create',
    },
    {
      id: 'Pipeline',
      title: 'Create pipeline',
      path: Routes.SYSTEM.PIPELINES.CREATE,
      permissions: 'pipeline:create',
    },
    {
      id: 'Pipeline Rule',
      title: 'Create pipeline rule',
      path: Routes.SYSTEM.PIPELINES.RULE('new?rule_builder=true'),
      permissions: 'pipeline:create',
      telemetryEvent: {
        type: TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.CREATE_RULE_CLICKED,
        section: 'pipeline-rules',
        actionValue: 'create-rule-button',
      },
    },
    {
      id: 'Content Pack',
      title: 'Create content pack',
      path: Routes.SYSTEM.CONTENTPACKS.CREATE,
    },
  ],
};

export default entityCreatorBindings;
