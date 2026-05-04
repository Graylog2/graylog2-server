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

// TODO(1.8 follow-up): once @graylog/server-api regenerates against Patrick's
// backend (graylog2-server#25873), flip the derivation from
// `['notifications'][number]` to `['elements'][number]` to match the new
// PageListResponse shape. Until then, we keep the legacy shape so the existing
// SDK calls continue to compile.
export type NotificationType = Awaited<
  ReturnType<(typeof SystemNotifications)['listNotifications']>
>['notifications'][number];

// Retention configuration form payload — see Phase 4 (NotificationsConfig.tsx)
// and Spec scenario `Configurable retention`. The backend exposes
// GET /system/notifications/config and PUT /system/notifications/config returning
// `{ retention_days: number }`. Once the SDK regenerates, prefer deriving this
// from `SystemNotifications` directly; for now the explicit shape lets Phase 4
// build against a mocked SDK.
export type SystemNotificationConfig = {
  retention_days: number;
};
