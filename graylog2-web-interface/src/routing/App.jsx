import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import Navigation from 'components/navigation/Navigation';
import Spinner from 'components/common/Spinner';
import Footer from 'components/layout/Footer';

import 'stylesheets/jquery.dynatable.css';
import 'stylesheets/typeahead.less';
import 'c3/c3.css';
import 'dc/dc.css';

import StoreProvider from 'injection/StoreProvider';
import AppErrorBoundary from './AppErrorBoundary';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const App = createReactClass({
  displayName: 'App',

  propTypes: {
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]).isRequired,
    location: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(CurrentUserStore)],

  render() {
    if (!this.state.currentUser) {
      return <Spinner />;
    }
    return (
      <div>
        <Navigation requestPath={this.props.location.pathname}
                    fullName={this.state.currentUser.full_name}
                    loginName={this.state.currentUser.username}
                    permissions={this.state.currentUser.permissions} />
        <div id="scroll-to-hint" style={{ display: 'none' }} className="alpha80">
          <i className="fa fa-arrow-up" />
        </div>
        <AppErrorBoundary>
          {this.props.children}
        </AppErrorBoundary>
        <Footer />
      </div>
    );
  },
});

export default App;
