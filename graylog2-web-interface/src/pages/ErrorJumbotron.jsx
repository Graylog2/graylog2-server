import React from 'react';
import PropTypes from 'prop-types';
import styled, { createGlobalStyle, css } from 'styled-components';
import { transparentize } from 'polished';

import { Col, Jumbotron, Row } from 'components/graylog';
import NotFoundBackgroundImage from 'assets/not-found-bg.jpg';

const GlobalStyle = createGlobalStyle`
  body {
    background: url(${NotFoundBackgroundImage}) no-repeat center center fixed;
    background-size: cover;
  }
`;

const ContainerRow = styled(Row)`
  height: 82vh;
`;

const StyledErrorJumbotron = styled(Jumbotron)(({ theme }) => css`
  background-color: ${transparentize(0.2, theme.color.global.contentBackground)};
  text-align: center;
`);

export const H1 = styled.h1`
  font-size: 52px;
  margin-bottom: 15px;
`;

const ErrorJumbotron = ({ children }) => {
  return (
    <ContainerRow>
      <GlobalStyle />
      <Col mdOffset={2} md={8}>
        <StyledErrorJumbotron>
          {children}
        </StyledErrorJumbotron>
      </Col>
    </ContainerRow>
  );
};

ErrorJumbotron.propTypes = {
  children: PropTypes.node.isRequired,
};

export default ErrorJumbotron;
