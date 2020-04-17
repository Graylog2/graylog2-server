import React from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import chroma from 'chroma-js';

import { Col, Jumbotron, Row } from 'components/graylog';

const ContainerRow = styled(Row)`
  height: 82vh;
`;

const StyledErrorJumbotron = styled(Jumbotron)(({ theme }) => css`
  backgrond-color: ${chroma(theme.color.global.contentBackground).alpha(0.2).css()};
  text-align: center;
`);

const ErrorJumbotron = ({ children }) => {
  return (
    <ContainerRow>
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
