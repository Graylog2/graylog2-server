import React from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import Icon from './Icon';

type Props = {
  bsStyle?: string,
  children: React.ReactNode,
  className?: string,
  icon?: string,
}

const Wrapper = styled.div`
  position: relative;
`;

const PositionIcon = styled(Icon)(({ $color, theme }) => css`
  position: absolute;
  top: 0;
  right: 0;
  transform: translate(25%, -25%);
  filter: drop-shadow(0 0 2px ${theme.colors.variant.lighter.default});
  font-size: ${theme.fonts.size.small};
  color: ${theme.colors.variant[$color]};
`);

const IconMarker = ({ bsStyle, children, className, icon }:Props) => {
  if (!icon) {
    return (
      <>
        {children}
      </>
    );
  }

  return (
    <Wrapper className={className}>
      <PositionIcon name={icon} $color={bsStyle} />
      {children}
    </Wrapper>
  );
};

IconMarker.propTypes = {
  bsStyle: PropTypes.string,
  children: PropTypes.node.isRequired,
  className: PropTypes.string,
  icon: PropTypes.string,
};

IconMarker.defaultProps = {
  bsStyle: 'default',
  className: undefined,
  icon: undefined,
};

export default IconMarker;
