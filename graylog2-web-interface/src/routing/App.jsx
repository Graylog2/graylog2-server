import PropTypes from 'prop-types';
import React from 'react';
import styled, { css } from 'styled-components';
import chroma from 'chroma-js';

import Navigation from 'components/navigation/Navigation';
import { Scratchpad, Icon, Spinner } from 'components/common';
import connect from 'stores/connect';
import StoreProvider from 'injection/StoreProvider';
import { ScratchpadProvider } from 'providers/ScratchpadProvider';

import ReportedErrorBoundary from 'components/errors/ReportedErrorBoundary';
import RuntimeErrorBoundary from 'components/errors/RuntimeErrorBoundary';

import 'stylesheets/typeahead.less';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');

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

const App = ({ children, currentUser, location }) => {
  if (!currentUser) {
    return <Spinner />;
  }

  return (
    <ScratchpadProvider loginName={currentUser.username}>
      <Navigation requestPath={location.pathname}
                  fullName={currentUser.full_name}
                  loginName={currentUser.username}
                  permissions={currentUser.permissions} />
      <ReportedErrorBoundary>
        <RuntimeErrorBoundary>
          <ScrollToHint id="scroll-to-hint">
            <Icon name="arrow-up" />
          </ScrollToHint>
          <Scratchpad />
          {children}
        </RuntimeErrorBoundary>
      </ReportedErrorBoundary>
    </ScratchpadProvider>
  );
};

App.propTypes = {
  children: PropTypes.oneOfType([
    PropTypes.arrayOf(PropTypes.element),
    PropTypes.element,
  ]).isRequired,
  currentUser: PropTypes.shape({
    full_name: PropTypes.string,
    username: PropTypes.string,
    permissions: PropTypes.array,
  }),
  location: PropTypes.object.isRequired,
};

App.defaultProps = {
  currentUser: undefined,
};

export default connect(App, { currentUser: CurrentUserStore }, ({ currentUser: { currentUser } = {} }) => ({ currentUser }));
