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
    const typeData = legacyTypes[callbackType];

    let content;
    if (typeData) {
      const typeConfiguration = typeData.configuration;

      content = Object.entries(typeConfiguration)
        .map(([key, value]) => {
          return (
            <tr key={key}>
              <td>{value.human_name}</td>
              <td>{configurationValues[key]}</td>
            </tr>
          );
        });
    } else {
      content = (
        <tr className="danger">
          <td>Type</td>
          <td>
            Unknown legacy alarm callback type: <strong>{callbackType}</strong> Please make sure the plugin is installed.
          </td>
        </tr>
      );
    }

    return (
      <CommonNotificationSummary {...this.props}>
        <React.Fragment>
          {content}
        </React.Fragment>
      </CommonNotificationSummary>
    );
  }
}

export default LegacyNotificationSummary;
