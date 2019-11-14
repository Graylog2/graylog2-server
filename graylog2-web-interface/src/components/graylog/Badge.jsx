import React, { forwardRef, useMemo } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Badge as BootstrapBadge } from 'react-bootstrap';

import { readableColor } from 'theme/utils';
import bsStyleThemeVariant, { bsStyles } from './variants/bsStyle';

const badgeStyles = () => {
  const cssBuilder = (hex) => {
    const backgroundColor = hex;
    const textColor = readableColor(backgroundColor);

    return css`
      background-color: ${backgroundColor};
      color: ${textColor};
    `;
  };

  return bsStyleThemeVariant(cssBuilder);
};

const Badge = forwardRef(({ bsStyle, ...props }, ref) => {
  const StyledBadge = useMemo(
    () => styled(BootstrapBadge)`${badgeStyles(props)}`,
    [bsStyle],
  );

  return (
    <StyledBadge ref={ref} {...props} />
  );
});

Badge.propTypes = {
  /* Bootstrap `bsStyle` variant name */
  bsStyle: PropTypes.oneOf(bsStyles),
};

Badge.defaultProps = {
  bsStyle: 'default',
};

export default Badge;
