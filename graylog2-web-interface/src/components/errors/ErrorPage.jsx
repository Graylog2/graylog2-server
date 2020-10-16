// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { createGlobalStyle, css } from 'styled-components';

import NotFoundBackgroundImage from 'assets/not-found-bg.jpg';
import AppContentGrid from 'components/layout/AppContentGrid';
import DocumentTitle from 'components/common/DocumentTitle';
import ErrorJumbotron from 'components/errors/ErrorJumbotron';

const generateStyles = () => css`
  body {
    background: url(${(props) => props.backgroundImage}) no-repeat center center fixed;
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
  children?: React.Node,
  description: React.Node,
  title: string,
};

const ErrorPageStyles = createGlobalStyle`
    ${generateStyles()}
  `;

const ErrorPage = ({ children, title, description, backgroundImage }: Props) => (
  <AppContentGrid>
    <ErrorPageStyles backgroundImage={backgroundImage} />
    <div className="container-fluid">
      <DocumentTitle title={title}>
        <ErrorJumbotron title={title}>
          {description}
          {children && (
          <ErrorMessage>
            {children}
          </ErrorMessage>
          )}
        </ErrorJumbotron>
      </DocumentTitle>
    </div>
  </AppContentGrid>
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
