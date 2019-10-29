import React, { forwardRef, useCallback } from 'react';
import styled from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Jumbotron as BootstrapJumbotron } from 'react-bootstrap';

import { useTheme } from 'theme/GraylogThemeContext';

const Jumbotron = forwardRef((props, ref) => {
  const { colors } = useTheme();
  const StyledJumbotron = useCallback(styled(BootstrapJumbotron)`
    color: ${colors.primary.tre};
    background-color: ${colors.primary.due};
  `, []);

  return (
    <StyledJumbotron ref={ref} {...props} />
  );
});

export default Jumbotron;
