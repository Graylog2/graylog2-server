import React from 'react';
import PropTypes from 'prop-types';
import { Col, Row } from 'components/graylog';
import { StyledJumbotron } from 'components/graylog/Jumbotron';
import styled, { createGlobalStyle, css } from 'styled-components';
import { rgba } from 'polished';

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

const StyledErrorJumbotron = styled(StyledJumbotron)(({ theme }) => css`
  && {
    background-color: ${rgba(theme.color.primary.due, 0.8)};
    text-align: center;
  }
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
