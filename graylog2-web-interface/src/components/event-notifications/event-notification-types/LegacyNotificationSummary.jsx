import React from 'react';
import PropTypes from 'prop-types';

import CommonNotificationSummary from './CommonNotificationSummary';

class LegacyNotificationSummary extends React.Component {
  static propTypes = {
    type: PropTypes.string.isRequired,
    notification: PropTypes.object.isRequired,
    definitionNotification: PropTypes.object.isRequired,
    legacyTypes: PropTypes.object.isRequired,
  };

  render() {
    const { notification, legacyTypes } = this.props;
    const configurationValues = notification.config.configuration;
    const callbackType = notification.config.callback_type;
    const typeConfiguration = legacyTypes[callbackType].configuration;

    const summaryFields = Object.entries(typeConfiguration).map(([key, value]) => {
      return (
        <tr key={key}>
          <td>{value.human_name}</td>
          <td>{configurationValues[key]}</td>
        </tr>
      );
    });

    return (
      <CommonNotificationSummary {...this.props}>
        <React.Fragment>
          {summaryFields}
        </React.Fragment>
      </CommonNotificationSummary>
    );
  }
}

export default LegacyNotificationSummary;
