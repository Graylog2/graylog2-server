import * as React from 'react';
import PropTypes from 'prop-types';

import { ReadOnlyFormGroup } from 'components/common';

const PagerDutyNotificationDetails = ({ notification }) => {
  return (
    <>
      <ReadOnlyFormGroup label="Routing Key" value={notification.config?.routing_key} />
      <ReadOnlyFormGroup label="Custom Incident" value={notification.config?.custom_incident} />
      <ReadOnlyFormGroup label="Key Prefix" value={notification.config?.key_prefix} />
      <ReadOnlyFormGroup label="Client Name" value={notification.config?.client_name} />
      <ReadOnlyFormGroup label="Client URL" value={notification.config?.client_url} />
    </>
  );
};

PagerDutyNotificationDetails.propTypes = {
  notification: PropTypes.shape({
    config: PropTypes.shape({
      routing_key: PropTypes.string,
      custom_incident: PropTypes.bool,
      key_prefix: PropTypes.string,
      client_name: PropTypes.string,
      client_url: PropTypes.string,
    }).isRequired,
  }).isRequired,
};

export default PagerDutyNotificationDetails;
