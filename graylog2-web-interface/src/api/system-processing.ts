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
import { ClusterProcessing } from '@graylog/server-api';

import i18n from 'i18n';
import UserNotification from 'util/UserNotification';

export const pauseProcessing = (nodeId: string): Promise<void> =>
  ClusterProcessing.pause(nodeId).then(
    () => {
      UserNotification.success(i18n.t('notifications.processing_pause_success', { ns: 'system', nodeId }));
    },
    (error: unknown) => {
      UserNotification.error(
        i18n.t('notifications.processing_pause_failed', { ns: 'system', nodeId, error }),
        i18n.t('notifications.processing_pause_failed_title', { ns: 'system', nodeId }),
      );
    },
  );

export const resumeProcessing = (nodeId: string): Promise<void> =>
  ClusterProcessing.resume(nodeId).then(
    () => {
      UserNotification.success(i18n.t('notifications.processing_resume_success', { ns: 'system', nodeId }));
    },
    (error: unknown) => {
      UserNotification.error(
        i18n.t('notifications.processing_resume_failed', { ns: 'system', nodeId, error }),
        i18n.t('notifications.processing_resume_failed_title', { ns: 'system', nodeId }),
      );
    },
  );
