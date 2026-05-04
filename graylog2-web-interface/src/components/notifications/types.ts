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
import type { SystemNotifications } from '@graylog/server-api';

// Derived from the new paginated endpoint (GET /system/notifications/paginated)
// per Patrick's backend in graylog2-server#25873. The DTO covers id, type,
// title, description, severity, key, actor, is_read, last_changed, triggered_at,
// node_id, and details.
export type NotificationType = Awaited<
  ReturnType<(typeof SystemNotifications)['getPaginated']>
>['elements'][number];

// Legacy shape returned by GET /system/notifications (the pre-paginated endpoint).
// Still used by `useNotifications()`, `NotificationsList.tsx`, and `Notification.tsx`.
// Phase 5 deletes those callers; this alias is the bridge until then.
export type LegacyNotificationType = Awaited<
  ReturnType<(typeof SystemNotifications)['listNotifications']>
>['notifications'][number];

// Retention configuration form payload — see Phase 4 (NotificationsConfig.tsx)
// and Spec scenario `Configurable retention`. Backend exposes
// GET /system/notifications/config and PUT /system/notifications/config.
// Derived from the SDK's SystemNotificationRetentionConfig.
export type SystemNotificationConfig = Awaited<
  ReturnType<(typeof SystemNotifications)['getConfig']>
>;
