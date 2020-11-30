/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { createGlobalStyle } from 'styled-components';

import { DocumentTitle, Spinner, Icon } from 'components/common';
import LoginBox from 'components/login/LoginBox';
import authStyles from 'theme/styles/authStyles';

type Props = {
  text: string,
};

const LoadingPageStyles = createGlobalStyle`
  ${authStyles}
`;

const LoadingPage = ({ text }: Props) => {
  return (
    <DocumentTitle title="Loading...">
      <LoadingPageStyles />
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
