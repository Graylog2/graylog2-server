import * as React from 'react';
import PropTypes from 'prop-types';

import { ReadOnlyFormGroup } from 'components/common';

const HttpNotificationDetails = ({ notification }) => {
  return (
    <>
      <ReadOnlyFormGroup label="URL" value={notification.config.url} />
    </>
  );
};

HttpNotificationDetails.propTypes = {
  notification: PropTypes.object.isRequired,
};

export default HttpNotificationDetails;
