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
import i18n from 'i18n';

import type { EventNotificationTypes } from './types';

/** Displayed name of a notification type. Plugins without `displayNameKey` show their `displayName`. */
const notificationTypeTitle = (
  plugin: Partial<Pick<EventNotificationTypes, 'displayName' | 'displayNameKey'>> | undefined,
) =>
  plugin?.displayNameKey ? i18n.t(plugin.displayNameKey, { defaultValue: plugin.displayName }) : plugin?.displayName;

export default notificationTypeTitle;
