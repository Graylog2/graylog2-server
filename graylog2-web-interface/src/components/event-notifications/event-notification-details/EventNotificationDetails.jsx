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
import * as React from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';
import PropTypes from 'prop-types';

import { ReadOnlyFormGroup } from 'components/common';
import SectionComponent from 'components/common/Section/SectionComponent';
import { Alert } from 'components/graylog';

const _getNotificationPlugin = (type) => {
  if (type === undefined) {
    return {};
  }

  return PluginStore.exports('eventNotificationTypes').find((n) => n.type === type) || {};
};

const EventNotificationDetails = ({ notification }) => {
  const notificationPlugin = _getNotificationPlugin(notification.config.type);
  const DetailsComponent = notificationPlugin?.detailsComponent;

  return (
    <SectionComponent title="Details">
      <ReadOnlyFormGroup label="Title" value={notification.title} />
      <ReadOnlyFormGroup label="Description" value={notification.description} />
      <ReadOnlyFormGroup label="Notification Type" value={notification.config.type} />
      {DetailsComponent ? <DetailsComponent notification={notification} /> : <Alert bsStyle="danger">Notification type not supported</Alert>}
    </SectionComponent>
  );
};

EventNotificationDetails.propTypes = {
  notification: PropTypes.object.isRequired,
};

export default EventNotificationDetails;
