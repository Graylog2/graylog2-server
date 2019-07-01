import React from 'react';
import PropTypes from 'prop-types';

import EmailNotificationForm from './EmailNotificationForm';

class EmailNotificationFormContainer extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  render() {
    return <EmailNotificationForm {...this.props} usernames={[]} />;
  }
}

export default EmailNotificationFormContainer;
