// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import type { StyledComponent } from 'styled-components';

import { Grid } from 'components/graylog';

type Props = {
  children: React.Node,
};

const Container: StyledComponent<Props, void, HTMLDivElement> = styled.div`
  padding: 15px 12px;
`;

const AppContentGrid = ({ children, ...rest }: Props) => (
  <Container {...rest}>
    <Grid fluid>
      {children}
    </Grid>
  </Container>
);

AppContentGrid.propTypes = {
  children: PropTypes.node.isRequired,
};

export default AppContentGrid;
