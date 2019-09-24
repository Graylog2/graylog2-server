import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Scratchpad } from '../components/common';

const GridContent = styled.div`
  display: grid;
  grid-template-rows: 1fr;
  grid-template-columns: 1fr 30px;
  grid-template-areas: "content scratchpad";
`;

const ContentArea = styled.div`
  grid-area: content;
`;

const ScratchpadWrapper = styled.div`
  grid-area: scratchpad;
  position: sticky;
  top: 50px;
  right: 0;
  z-index: 10;
  height: calc(100vh - 50px);
  background-color: #393939;
`;

const AppWithScratchpad = ({ children }) => {
  return (
    <GridContent>
      <ContentArea>
        {children}
      </ContentArea>

      <ScratchpadWrapper>
        <Scratchpad />
      </ScratchpadWrapper>
    </GridContent>
  );
};

AppWithScratchpad.propTypes = {
  children: PropTypes.oneOfType([
    PropTypes.arrayOf(PropTypes.element),
    PropTypes.element,
  ]).isRequired,
};

export default AppWithScratchpad;
