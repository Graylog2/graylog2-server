import React, { forwardRef, useMemo } from 'react';
import styled from 'styled-components';
import { color } from 'theme';
// eslint-disable-next-line no-restricted-imports
import { Jumbotron as BootstrapJumbotron } from 'react-bootstrap';

const Jumbotron = forwardRef((props, ref) => {
  const StyledJumbotron = useMemo(
    () => {
      return styled(BootstrapJumbotron)`
        color: ${color.global.textDefault};
        background-color: ${color.gray[100]};
      `;
    }, [],
  );

  return (
    <StyledJumbotron ref={ref} {...props} />
  );
});

export default Jumbotron;
