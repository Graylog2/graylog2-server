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
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import { FieldContentConditionSummary } from 'components/alertconditions/fieldcontentcondition';
import { FieldValueConditionSummary } from 'components/alertconditions/fieldvaluecondition';
import { MessageCountConditionSummary } from 'components/alertconditions/messagecountcondition';

export { default as AlertCondition } from './AlertCondition';
export { default as AlertConditionForm } from './AlertConditionForm';
export { default as AlertConditionSummary } from './AlertConditionSummary';
export { default as AlertConditionsComponent } from './AlertConditionsComponent';
export { default as AlertConditionsList } from './AlertConditionsList';
export { default as AlertConditionTestModal } from './AlertConditionTestModal';
export { default as CreateAlertConditionInput } from './CreateAlertConditionInput';
export { default as EditAlertConditionForm } from './EditAlertConditionForm';
export { default as GenericAlertConditionSummary } from './GenericAlertConditionSummary';
export { default as StreamAlertConditions } from './StreamAlertConditions';
export { default as UnknownAlertCondition } from './UnknownAlertCondition';

PluginStore.register(new PluginManifest({}, {
  alertConditions: [
    {
      summaryComponent: FieldContentConditionSummary,
      type: 'field_content_value',
    },
    {
      summaryComponent: FieldValueConditionSummary,
      type: 'field_value',
    },
    {
      summaryComponent: MessageCountConditionSummary,
      type: 'message_count',
    },
  ],
}));
