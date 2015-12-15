import React, {PropTypes} from 'react';
import Reflux from 'reflux';

import { Spinner } from 'components/common';
import Routes from 'routing/Routes';

import PermissionsMixin from 'util/PermissionsMixin';

import CurrentUserStore from 'stores/users/CurrentUserStore';
import GettingStartedStore from 'stores/gettingstarted/GettingStartedStore';

import GettingStartedActions from 'actions/gettingstarted/GettingStartedActions';

const StartPage = React.createClass({
  propTypes: {
    history: PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(CurrentUserStore), Reflux.listenTo(GettingStartedStore, 'onGettingStartedUpdate')],
  getInitialState() {
    return {
      gettingStarted: undefined,
    };
  },
  componentDidMount() {
    GettingStartedActions.getStatus();
  },
  onGettingStartedUpdate(state) {
    this.setState({gettingStarted: state.status});
  },
  _redirect() {
    if (PermissionsMixin.isPermitted(this.state.currentUser.permissions, ['INPUTS_CREATE'])) {
      if (!!this.state.gettingStarted.show) {
        this.props.history.pushState(null, Routes.GETTING_STARTED);
        return;
      }
    }

    this.props.history.pushState(null, Routes.SEARCH);
  },
  _isLoading() {
    return !this.state.currentUser || !this.state.gettingStarted;
  },
  render() {
    if (!this._isLoading()) {
      this._redirect();
    }
    return <Spinner/>;
  },
});

export default StartPage;
