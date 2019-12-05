import React, { useMemo } from 'react';
import PropTypes from 'prop-types';
import { Col, Jumbotron, Row } from 'components/graylog';
import styled, { createGlobalStyle } from 'styled-components';
import { rgba } from 'polished';

import { useTheme } from 'theme/GraylogThemeContext';
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

const StyledJumbotron = color => useMemo(
  () => {
    return styled(Jumbotron)`
      && {
        background-color: ${rgba(color, 0.8)};
        text-align: center;
      }
    `;
  },
  [color],
);

export const H1 = styled.h1`
  font-size: 52px;
  margin-bottom: 15px;
`;

const ErrorJumbotron = ({ children }) => {
  const { colors } = useTheme();

  const StyledJumbo = StyledJumbotron(colors.primary.due);

  return (
    <ContainerRow>
      <GlobalStyle />
      <Col mdOffset={2} md={8}>
        <StyledJumbo>
          {children}
        </StyledJumbo>
      </Col>
    </ContainerRow>
  );
};

ErrorJumbotron.propTypes = {
  children: PropTypes.node.isRequired,
};

export default ErrorJumbotron;
