import React from 'react';
import PropTypes from 'prop-types';
import { Box } from 'react-mops';
import styled from 'styled-components';

import 'react-mops/dist/esm/index.css';

const MopsBox = styled(Box).attrs(
  ({ size }) => ({
    width: `${size.width}px`,
    height: `${size.height}px`,
  }),
)`
  overflow: hidden;
  box-shadow: 0 0 3px rgba(0, 0, 0, .25);
  background-color: #393939;
  border-radius: ${({ opened }) => (opened ? '3px' : '50%')};
`;

const InvisibleMarker = styled.span`
  position: absolute;
  pointer-events: none;
  top: 0;
  left: 0;
  height: 0;
  width: 0;
`;

const Mops = ({ children, size, ...props }) => {
  return (
    <MopsBox size={size}
             marker={InvisibleMarker}
             fullHandles={false}
             hideGuides={() => true}
             drawBox={false}
             {...props}>
      {children}
    </MopsBox>
  );
};

Mops.propTypes = {
  children: PropTypes.node.isRequired,
  height: PropTypes.number,
  width: PropTypes.number,
  size: PropTypes.shape({
    height: PropTypes.number.isRequired,
    width: PropTypes.number.isRequired,
  }).isRequired,
};

Mops.defaultProps = {
  height: 50,
  width: 50,
};

export default Mops;
