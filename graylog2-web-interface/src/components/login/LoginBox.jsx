import React from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import { Col, Row } from 'components/graylog';

const LoginCol = styled(Col)(({ theme }) => css`
  padding: 15px;
  background-color: ${theme.color.primary.due};
  border: 1px solid ${theme.color.secondary.tre};
  border-radius: 4px;
  box-shadow: 0 0 21px rgba(0, 0, 0, 0.75);
  margin-top: 120px;
`);

const LoginBox = ({ children }) => {
  return (
    <div className="container">
      <Row>
        <LoginCol md={4} mdOffset={4} xs={6} xsOffset={3}>
          {children}
        </LoginCol>
      </Row>
    </div>
  );
};

LoginBox.propTypes = {
  children: PropTypes.node.isRequired,
};

export default LoginBox;
