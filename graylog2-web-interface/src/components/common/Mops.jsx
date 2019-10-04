import React from 'react';
import PropTypes from 'prop-types';
import { Box } from 'react-mops';
import styled from 'styled-components';

import 'react-mops/dist/esm/index.css';

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
    <Box size={size}
         marker={InvisibleMarker}
         fullHandles={false}
         hideGuides={() => true}
         drawBox={false}
         {...props}>
      {children}
    </Box>
  );
};

Mops.propTypes = {
  children: PropTypes.node.isRequired,
  size: PropTypes.shape({
    height: PropTypes.number.isRequired,
    width: PropTypes.number.isRequired,
  }).isRequired,
};

export default Mops;
