// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { createGlobalStyle } from 'styled-components';

import AppContentGrid from 'components/layout/AppContentGrid';
import { DocumentTitle, ErrorJumbotron } from 'components/common';

const GlobalStyle = createGlobalStyle`
  body {
    background: url(${(props) => props.backgroundImage}) no-repeat center center fixed;
    background-size: cover;
  }
`;

const ErrorMessage = styled.div`
  margin-left: auto;
  margin-right: auto;
  text-align: left;
  dt {
    font-size: 1.2em;
    font-weight: normal;
    overflow: auto;
  }
`;

const H1 = styled.h1`
  font-size: 52px;
  margin-bottom: 15px;
`;

type Props = {
  backgroundImage?: string,
  children?: React.Node,
  description: React.Node,
  title: string,
}

const ReportedErrorDetails = ({ children, title, description, backgroundImage }: Props) => (
  <AppContentGrid>
    {backgroundImage && <GlobalStyle backgroundImage={backgroundImage} />}
    <div className="container-fluid">
      <DocumentTitle title={title}>
        <ErrorJumbotron>
          <H1>{title}</H1>
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

ReportedErrorDetails.propTypes = {
  children: PropTypes.node,
  description: PropTypes.node.isRequired,
  title: PropTypes.string.isRequired,
  backgroundImage: PropTypes.string,
};

ReportedErrorDetails.defaultProps = {
  children: undefined,
  backgroundImage: undefined,
};

export default ReportedErrorDetails;
