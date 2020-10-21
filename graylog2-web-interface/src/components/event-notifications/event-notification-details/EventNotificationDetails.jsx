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
