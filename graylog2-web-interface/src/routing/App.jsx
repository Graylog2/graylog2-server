import PropTypes from 'prop-types';
import React from 'react';
import { inject, observer } from 'mobx-react';
import Navigation from 'components/navigation/Navigation';
import Footer from 'components/layout/Footer';

import 'stylesheets/jquery.dynatable.css';
import 'stylesheets/typeahead.less';
import 'c3/c3.css';
import 'dc/dc.css';

import StoreProvider from 'injection/StoreProvider';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const App = React.createClass({
  propTypes: {
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]).isRequired,
    currentUser: PropTypes.object.isRequired,
    location: PropTypes.object.isRequired,
  },

  render() {
    const currentUser = this.props.currentUser;
    return (
      <div>
        <Navigation requestPath={this.props.location.pathname}
                    fullName={currentUser.full_name}
                    loginName={currentUser.username}
                    permissions={currentUser.permissions} />
        <div id="scroll-to-hint" style={{ display: 'none' }} className="alpha80">
          <i className="fa fa-arrow-up" />
        </div>
        {this.props.children}
        <Footer />
      </div>
    );
  },
});

export default inject(() => ({
  currentUser: CurrentUserStore.currentUser,
}))(observer(App));
