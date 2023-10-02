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
import React from 'react';

import CommonNotificationSummary from './CommonNotificationSummary';

type Props = {
  type: string,
  notification: any,
  definitionNotification: any,
};

const HttpNotificationSummaryV2 = ({ type, notification, definitionNotification } : Props) => (
  <CommonNotificationSummary type={type}
                             notification={notification}
                             definitionNotification={definitionNotification}>
    <tr>
      <td>URL</td>
      <td><code>{notification.config.url}</code></td>
    </tr>
    <tr>
      <td>Method</td>
      <td><code>{notification.config.method}</code></td>
    </tr>
    <tr>
      <td>Content Type</td>
      <td><code>{notification.config.content_type}</code></td>
    </tr>
    <tr>
      <td>Body Template</td>
      <td><code>{notification.config.body_template}</code></td>
    </tr>
  </CommonNotificationSummary>
);

export default HttpNotificationSummaryV2;
