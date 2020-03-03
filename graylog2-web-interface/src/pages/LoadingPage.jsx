import PropTypes from 'prop-types';
import React from 'react';

import { DocumentTitle, Spinner, Icon } from 'components/common';
import LoginBox from 'components/login/LoginBox';
import AuthThemeStyles from 'theme/styles/authStyles';

const LoadingPage = ({ text }) => {
  return (
    <DocumentTitle title="Loading...">
      <AuthThemeStyles />
      <LoginBox>
        <legend><Icon name="group" /> Welcome to Graylog</legend>
        <p>
          <Spinner text={text} />
        </p>
      </LoginBox>
    </DocumentTitle>
  );
};
LoadingPage.propTypes = {
  text: PropTypes.string,
};
LoadingPage.defaultProps = {
  text: 'Loading, please wait...',
};

export default LoadingPage;
