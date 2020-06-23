import PropTypes from 'prop-types';
import React from 'react';
import styled, { css } from 'styled-components';
import chroma from 'chroma-js';

import { Scratchpad, Icon, Spinner } from 'components/common';
import { ScratchpadProvider } from 'providers/ScratchpadProvider';
import CurrentUserContext from 'contexts/CurrentUserContext';
import Navigation from 'components/navigation/Navigation';
import ReportedErrorBoundary from 'components/errors/ReportedErrorBoundary';
import RuntimeErrorBoundary from 'components/errors/RuntimeErrorBoundary';

import 'stylesheets/typeahead.less';

const ScrollToHint = styled.div(({ theme }) => css`
  position: fixed;
  left: 50%;
  margin-left: -125px;
  top: 50px;
  color: ${theme.utils.readableColor(chroma(theme.colors.brand.tertiary).alpha(0.8).css())};
  font-size: 80px;
  padding: 25px;
  z-index: 2000;
  width: 200px;
  text-align: center;
  cursor: pointer;
  border-radius: 10px;
  display: none;
  background: ${chroma(theme.colors.brand.tertiary).alpha(0.8).css()};
`);

const App = ({ children, location }) => (
  <CurrentUserContext.Consumer>
    {(currentUser) => {
      if (!currentUser) {
        return <Spinner />;
      }
      return (
        <ScratchpadProvider loginName={currentUser.username}>
          <Navigation requestPath={location.pathname}
                      fullName={currentUser.full_name}
                      loginName={currentUser.username}
                      permissions={currentUser.permissions} />
          <ScrollToHint id="scroll-to-hint">
            <Icon name="arrow-up" />
          </ScrollToHint>
          <Scratchpad />
          <ReportedErrorBoundary>
            <RuntimeErrorBoundary>
              {children}
            </RuntimeErrorBoundary>
          </ReportedErrorBoundary>
        </ScratchpadProvider>
      );
    }}
  </CurrentUserContext.Consumer>
);

App.propTypes = {
  children: PropTypes.oneOfType([
    PropTypes.arrayOf(PropTypes.element),
    PropTypes.element,
  ]).isRequired,
  location: PropTypes.object.isRequired,
};

export default App;
