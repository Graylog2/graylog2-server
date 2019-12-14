import PropTypes from 'prop-types';
import React from 'react';

import Navigation from 'components/navigation/Navigation';
import { Scratchpad, Icon, Spinner } from 'components/common';
import connect from 'stores/connect';
import StoreProvider from 'injection/StoreProvider';
import { ScratchpadProvider } from 'providers/ScratchpadProvider';

import AppErrorBoundary from './AppErrorBoundary';

import 'stylesheets/jquery.dynatable.css';
import 'stylesheets/typeahead.less';
import 'c3/c3.css';
import 'dc/dc.css';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');

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
      <div id="scroll-to-hint" style={{ display: 'none' }} className="alpha80">
        <Icon name="arrow-up" />
      </div>
      <Scratchpad />
      <AppErrorBoundary>
        {children}
      </AppErrorBoundary>
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
