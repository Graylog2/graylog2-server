import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Col, Row } from 'components/graylog';
import Footer from 'components/layout/Footer';

const StyledRow = styled(Row)`
  margin-bottom: 0;
`;

const StyledCol = styled(Col)`
  margin-top: 10px;
  padding: 5px 25px;

  @media print {
    width: 100%;
  }
`;

const AppWithoutSearchBar = (props) => {
  const { children } = props;
  return (
    <div className="container-fluid">
      <StyledRow>
        <StyledCol md={12}>
          {children}
        </StyledCol>
      </StyledRow>
      <Footer />
    </div>
  );
};

AppWithoutSearchBar.propTypes = {
  children: PropTypes.node.isRequired,
};

export default AppWithoutSearchBar;
