import * as React from 'react';
import { useEffect, useState } from 'react';
import PropTypes from 'prop-types';

import CombinedProvider from 'injection/CombinedProvider';
import { ReadOnlyFormGroup, Spinner } from 'components/common';
import { Alert, Well } from 'components/graylog';

import emailStyles from '../event-notification-types/EmailNotificationSummary.css';
import notificationStyles from '../event-notification-types/LegacyNotificationCommonStyles.css';

const { EventNotificationsActions } = CombinedProvider.get('EventNotifications');

const LegacyNotificationDetails = ({ notification }) => {
  const [legacyTypes, setLegacyTypes] = useState();
  const configurationValues = notification.config.configuration;
  const callbackType = notification.config.callback_type;
  const typeData = legacyTypes?.[callbackType];

  useEffect(() => {
    EventNotificationsActions.listAllLegacyTypes().then((result) => setLegacyTypes(result.types));
  }, []);

  if (!legacyTypes) {
    return <p><Spinner text="Loading legacy notification information..." /></p>;
  }

  return (
    <>
      {!typeData && (
        <Alert bsStyle="danger" className={notificationStyles.legacyNotificationAlert}>
          Error in {notification.title || 'Legacy Alarm Callback'}: Unknown type <code>{callbackType}</code>,
          please ensure the plugin is installed.
        </Alert>
      )}
      {typeData && Object.entries(typeData.configuration).map(([key, value]) => {
        if (key === 'body' || key === 'script_args') {
          return (
            <ReadOnlyFormGroup label={value.human_name}
                               value={(
                                 <Well bsSize="small" className={emailStyles.bodyPreview}>
                                   {configurationValues[key] || <em>Empty body</em>}
                                 </Well>
                               )} />
          );
        }

        return <ReadOnlyFormGroup label={value.human_name} value={configurationValues[key]} />;
      })}
    </>
  );
};

LegacyNotificationDetails.propTypes = {
  notification: PropTypes.object.isRequired,
};

export default LegacyNotificationDetails;
