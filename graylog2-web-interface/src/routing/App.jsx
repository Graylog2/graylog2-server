import PropTypes from 'prop-types';
import React from 'react';
import styled, { css } from 'styled-components';

import Navigation from 'components/navigation/Navigation';
import { Scratchpad, Icon, Spinner } from 'components/common';
import connect from 'stores/connect';
import StoreProvider from 'injection/StoreProvider';
import { ScratchpadProvider } from 'providers/ScratchpadProvider';

import ReportedError from 'components/errors/ReportedError';
import RuntimeErrorBoundary from 'components/errors/RuntimeErrorBoundary';

import 'stylesheets/typeahead.less';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const ScrollToHint = styled.div(({ theme }) => css`
  position: fixed;
  left: 50%;
  margin-left: -125px;
  top: 50px;
  color: ${theme.color.global.textAlt};
  font-size: 80px;
  padding: 25px;
  z-index: 2000;
  width: 200px;
  text-align: center;
  cursor: pointer;
  border-radius: 10px;
  display: none;
  background: rgba(0, 0, 0, 0.8);
  filter: ~"progid:DXImageTransform.Microsoft.gradient(startColorstr=#99000000, endColorstr=#99000000)";
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
      <ReportedError>
        <RuntimeErrorBoundary>
          <ScrollToHint id="scroll-to-hint">
            <Icon name="arrow-up" />
          </ScrollToHint>
          <Scratchpad />
          {children}
        </RuntimeErrorBoundary>
      </ReportedError>
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
