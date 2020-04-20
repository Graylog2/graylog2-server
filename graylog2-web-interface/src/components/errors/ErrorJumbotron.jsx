import React from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import chroma from 'chroma-js';

import { Col, Jumbotron, Row } from 'components/graylog';

const H1 = styled.h1`
  font-size: 52px;
  margin-bottom: 15px;
`;

const ContainerRow = styled(Row)`
  height: 82vh;
`;

const StyledErrorJumbotron = styled(Jumbotron)(({ theme }) => css`
  background-color: ${chroma(theme.color.global.contentBackground).alpha(0.8).css()};
  text-align: center;
`);

const ErrorJumbotron = ({ children, title }) => {
  return (
    <ContainerRow>
      <Col mdOffset={2} md={8}>
        <StyledErrorJumbotron>
          <H1>{title}</H1>
          {children}
        </StyledErrorJumbotron>
      </Col>
    </ContainerRow>
  );
};

ErrorJumbotron.propTypes = {
  children: PropTypes.node.isRequired,
  title: PropTypes.string.isRequired,
};

export default ErrorJumbotron;
