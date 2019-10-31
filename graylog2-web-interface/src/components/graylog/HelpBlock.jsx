import React, { forwardRef, useCallback } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { lighten } from 'polished';
// eslint-disable-next-line no-restricted-imports
import { HelpBlock as BootstrapHelpBlock } from 'react-bootstrap';

import { useTheme } from 'theme/GraylogThemeContext';

const HelpBlock = forwardRef(({ children, ...props }, ref) => {
  const { colors } = useTheme();

  const StyledHelpBlock = useCallback(styled(BootstrapHelpBlock)`
    display: block;
    margin-top: 5px;
    margin-bottom: 10px;
    color: ${lighten(0.50, colors.primary.tre)};
  `, []);

  return (
    <StyledHelpBlock ref={ref} {...props}>{children}</StyledHelpBlock>
  );
});

HelpBlock.propTypes = {
  children: PropTypes.node,
};

HelpBlock.defaultProps = {
  children: undefined,
};

export default HelpBlock;
