import React from 'react';
import PropTypes from 'prop-types';

import CommonNotificationSummary from './CommonNotificationSummary';

class EmailNotificationSummary extends React.Component {
  static propTypes = {
    type: PropTypes.string.isRequired,
    notification: PropTypes.object,
    definitionNotification: PropTypes.object.isRequired,
    legacyTypes: PropTypes.object.isRequired,
  };

  static defaultProps = {
    notification: {},
  };

  render() {
    const { notification, legacyTypes } = this.props;
    const configurationValues = notification.config.configuration;
    const callbackType = notification.config.callback_type;
    const typeConfiguration = legacyTypes[callbackType].configuration;

    const summaryFields = Object.entries(typeConfiguration).map((keyAndValue) => {
      const [key, value] = keyAndValue;
      return (
        <tr>
          <td>{value.human_name}</td>
          <td>{configurationValues[key]}</td>
        </tr>
      );
    });

    return (
      <CommonNotificationSummary {...this.props}>
        {summaryFields}
      </CommonNotificationSummary>
    );
  }
}

export default EmailNotificationSummary;
