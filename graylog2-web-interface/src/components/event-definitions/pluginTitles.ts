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
import type { FieldValueProvider } from 'graylog-web-plugin';

import i18n from 'i18n';
import AppConfig from 'util/AppConfig';
import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';
import { DEFAULT_PRODUCT_NAME } from 'brand-customization/useProductName';

import type { EventDefinitionType } from './types';

/** Displayed name of an event definition type. Plugins without `displayNameKey` show their `displayName`. */
export const eventDefinitionTypeTitle = (
  plugin: Partial<Pick<EventDefinitionType, 'displayName' | 'displayNameKey'>> | undefined,
) => (plugin?.displayNameKey ? i18n.t(plugin.displayNameKey) : plugin?.displayName);

/** Displayed description of an event definition type. Plugins without `descriptionKey` show their `description`. */
export const eventDefinitionTypeDescription = (
  plugin: Partial<Pick<EventDefinitionType, 'description' | 'descriptionKey'>> | undefined,
) =>
  plugin?.descriptionKey
    ? i18n.t(plugin.descriptionKey, { productName: AppConfig.branding?.()?.product_name ?? DEFAULT_PRODUCT_NAME })
    : plugin?.description;

const FIELD_VALUE_PROVIDER_TITLE_KEYS: { [type: string]: string } = {
  'template-v1': 'field_value_providers.template.title',
  'lookup-v1': 'field_value_providers.lookup_table.title',
};

/** Displayed name of a field value provider. Unknown (plugin) providers show their `displayName`. */
export const fieldValueProviderTitle = (plugin: Pick<FieldValueProvider, 'type' | 'displayName'> | undefined) => {
  const key = FIELD_VALUE_PROVIDER_TITLE_KEYS[plugin?.type];

  return key ? i18n.t(key, { ns: 'event_definitions' }) : plugin?.displayName;
};

/** Translated name of an event definition priority, e.g. "High". Unknown priorities are shown as they are. */
export const priorityTitle = (priority: number | string) => {
  const properties = EventDefinitionPriorityEnum.properties[priority];

  return properties ? i18n.t(`priorities.${properties.name}`, { ns: 'event_definitions' }) : String(priority);
};
