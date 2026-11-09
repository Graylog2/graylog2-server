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
import type { EventNotificationTypes } from 'components/event-notifications/types';
import type { IsPermitted } from 'hooks/usePermissions';

// The type of a notification being edited stays listed so it still renders for users without the permissions.
const notificationTypeOptions = (
  types: Array<EventNotificationTypes>,
  isPermitted: IsPermitted,
  currentType: string | undefined,
) =>
  types
    .filter((type) => type.type === currentType || !type.requiredPermissions || isPermitted(type.requiredPermissions))
    .map((type) => ({ label: type.displayName, value: type.type }));

export default notificationTypeOptions;
