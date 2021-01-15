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
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { createGlobalStyle, css } from 'styled-components';

import NotFoundBackgroundImage from 'assets/not-found-bg.jpg';
import PageContentLayout from 'components/layout/PageContentLayout';
import DocumentTitle from 'components/common/DocumentTitle';
import ErrorJumbotron from 'components/errors/ErrorJumbotron';

const generateStyles = () => css`
  body {
    background: url(${({ backgroundImage }: { backgroundImage: string }) => backgroundImage}) no-repeat center center fixed;
    background-size: cover;
  }
`;

const ErrorMessage = styled.div(({ theme }) => css`
  margin-left: auto;
  margin-right: auto;
  text-align: left;

  dt {
    font-size: ${theme.fonts.size.body};
    font-weight: normal;
    overflow: auto;
  }

  p {
    font-size: inherit;
  }
`);

type Props = {
  backgroundImage?: string,
  children?: React.ReactNode,
  description: React.ReactNode,
  title: string,
};

const ErrorPageStyles = createGlobalStyle`
    ${generateStyles()}
  `;

const ErrorPage = ({ children, title, description, backgroundImage }: Props) => (
  <DocumentTitle title={title}>
    <PageContentLayout>
      <ErrorPageStyles backgroundImage={backgroundImage} />
      <ErrorJumbotron title={title}>
        {description}
        {children && (
          <ErrorMessage>
            {children}
          </ErrorMessage>
        )}
      </ErrorJumbotron>
    </PageContentLayout>
  </DocumentTitle>
);

ErrorPage.propTypes = {
  children: PropTypes.node,
  description: PropTypes.node.isRequired,
  title: PropTypes.string.isRequired,
  backgroundImage: PropTypes.string,
};

ErrorPage.defaultProps = {
  children: undefined,
  backgroundImage: NotFoundBackgroundImage,
};

export default ErrorPage;
