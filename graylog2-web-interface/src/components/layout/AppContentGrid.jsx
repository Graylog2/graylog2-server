// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { type StyledComponent } from 'styled-components';

import { Grid } from 'components/graylog';

type Props = {
  children: React.Node
}

const Container: StyledComponent<Props, {}, HTMLDivElement> = styled.div`
  padding: 15px 10px;
`;

const AppContentGrid = ({ children }: Props) => (
  <Container>
    <Grid fluid>
      {children}
    </Grid>
  </Container>
);

AppContentGrid.propTypes = {
  children: PropTypes.node.isRequired,
};

export default AppContentGrid;
