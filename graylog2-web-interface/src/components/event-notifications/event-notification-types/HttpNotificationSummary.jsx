import React from 'react';
import PropTypes from 'prop-types';

import CommonNotificationSummary from './CommonNotificationSummary';

class HttpNotificationSummary extends React.Component {
  static propTypes = {
    type: PropTypes.string.isRequired,
    notification: PropTypes.object,
    definitionNotification: PropTypes.object.isRequired,
  };

  static defaultProps = {
    notification: {},
  };

  render() {
    const { notification } = this.props;

    return (
      <CommonNotificationSummary {...this.props}>
        <>
          <tr>
            <td>URL</td>
            <td><code>{notification.config.url}</code></td>
          </tr>
        </>
      </CommonNotificationSummary>
    );
  }
}

export default HttpNotificationSummary;
