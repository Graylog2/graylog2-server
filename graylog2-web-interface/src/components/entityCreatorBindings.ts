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

import i18n from 'i18n';
import Routes from 'routing/Routes';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

const entityCreatorBindings: PluginExports = {
  'entityCreators': [
    {
      id: 'Event Definition',
      // Read lazily, so the title is translated when it is displayed and not when the module loads.
      get title() {
        return i18n.t('entity_creators.event_definition', { ns: 'common' });
      },
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
      // Read lazily, so the title is translated when it is displayed and not when the module loads.
      get title() {
        return i18n.t('entity_creators.notification', { ns: 'common' });
      },
      path: Routes.ALERTS.NOTIFICATIONS.CREATE,
      permissions: 'eventnotifications:create',
    },
    {
      id: 'Dashboard',
      // Read lazily, so the title is translated when it is displayed and not when the module loads.
      get title() {
        return i18n.t('entity_creators.dashboard', { ns: 'common' });
      },
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
      // Read lazily, so the title is translated when it is displayed and not when the module loads.
      get title() {
        return i18n.t('entity_creators.stream', { ns: 'common' });
      },
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
      // Read lazily, so the title is translated when it is displayed and not when the module loads.
      get title() {
        return i18n.t('entity_creators.index_set', { ns: 'common' });
      },
      path: Routes.SYSTEM.INDEX_SETS.CREATE,
      permissions: 'indexsets:create',
    },
    {
      id: 'User',
      // Read lazily, so the title is translated when it is displayed and not when the module loads.
      get title() {
        return i18n.t('entity_creators.user', { ns: 'common' });
      },
      path: Routes.SYSTEM.USERS.CREATE,
      permissions: 'users:create',
    },
    {
      id: 'Pipeline',
      // Read lazily, so the title is translated when it is displayed and not when the module loads.
      get title() {
        return i18n.t('entity_creators.pipeline', { ns: 'common' });
      },
      path: Routes.SYSTEM.PIPELINES.CREATE,
      permissions: 'pipeline:create',
    },
    {
      id: 'Pipeline Rule',
      // Read lazily, so the title is translated when it is displayed and not when the module loads.
      get title() {
        return i18n.t('entity_creators.pipeline_rule', { ns: 'common' });
      },
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
      // Read lazily, so the title is translated when it is displayed and not when the module loads.
      get title() {
        return i18n.t('entity_creators.content_pack', { ns: 'common' });
      },
      path: Routes.SYSTEM.CONTENTPACKS.CREATE,
    },
  ],
};

export default entityCreatorBindings;
