import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import Scratchpad from '../views/components/common/Scratchpad';

import AppGlobalNotifications from './AppGlobalNotifications';

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
    z-index: 2;
    height: calc(100vh - 50px);
    background-color: #393939;
  `;

const AppWithGlobalNotifications = ({ children }) => {
  return (
    <div>
      <AppGlobalNotifications />
      <GridContent>
        <ContentArea>
          {children}
        </ContentArea>

        <ScratchpadWrapper>
          <Scratchpad />
        </ScratchpadWrapper>
      </GridContent>
    </div>
  );
};

AppWithGlobalNotifications.propTypes = {
  children: PropTypes.oneOfType([
    PropTypes.arrayOf(PropTypes.element),
    PropTypes.element,
  ]).isRequired,
};

export default AppWithGlobalNotifications;
