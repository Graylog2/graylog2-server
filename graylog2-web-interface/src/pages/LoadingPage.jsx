import PropTypes from 'prop-types';
import React from 'react';

import { DocumentTitle, Spinner, Icon } from 'components/common';
import LoginBox from 'components/login/LoginBox';
import AuthThemeStyles from 'theme/styles/authStyles';
import DisconnectedThemeStyles from 'theme/styles/disconnectedStyles';

class LoadingPage extends React.Component {
  static propTypes = {
    text: PropTypes.string,
  };

  static defaultProps = {
    text: 'Loading, please wait...',
  };

  render() {
    const { text } = this.props;

    return (
      <DocumentTitle title="Loading...">
        <DisconnectedThemeStyles />
        <AuthThemeStyles />
        <LoginBox>
          <legend><Icon name="group" /> Welcome to Graylog</legend>
          <p className="loading-text">
            <Spinner text={text} />
          </p>
        </LoginBox>
      </DocumentTitle>
    );
  }
}

export default LoadingPage;
