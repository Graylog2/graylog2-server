import PropTypes from 'prop-types';
import React from 'react';

import { DocumentTitle, Spinner, Icon } from 'components/common';
import LoginBox from 'components/login/LoginBox';
import authStyles from 'theme/styles/authStyles';
import GlobalThemeStyles from 'theme/GlobalThemeStyles';

const LoadingPage = ({ text }) => {
  return (
    <DocumentTitle title="Loading...">
      <GlobalThemeStyles additionalStyles={authStyles} />
      <LoginBox>
        <legend><Icon name="users" /> Welcome to Graylog</legend>
        <p>
          <Spinner text={text} delay={0} />
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
