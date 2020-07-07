// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Col, Row } from 'components/graylog';
import Footer from 'components/layout/Footer';
import AppContentGrid from 'components/layout/AppContentGrid';

type Props = {
  children: React.Node,
};

const StyledRow = styled(Row)`
  margin-bottom: 0;
`;

const AppWithoutSearchBar = ({ children }: Props) => (
  <AppContentGrid>
    <StyledRow>
      <Col md={12}>
        {children}
      </Col>
    </StyledRow>
    <Footer />
  </AppContentGrid>
);

AppWithoutSearchBar.propTypes = {
  children: PropTypes.node.isRequired,
};

export default AppWithoutSearchBar;
