import React from 'react';
import Reflux from 'reflux';

import { Spinner } from 'components/common';
import Routes from 'routing/Routes';

import PermissionsMixin from 'util/PermissionsMixin';

import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');
const GettingStartedStore = StoreProvider.getStore('GettingStarted');

import ActionsProvider from 'injection/ActionsProvider';
const GettingStartedActions = ActionsProvider.getActions('GettingStarted');

const StartPage = React.createClass({
  propTypes: {
    history: React.PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(CurrentUserStore), Reflux.listenTo(GettingStartedStore, 'onGettingStartedUpdate')],
  getInitialState() {
    return {
      gettingStarted: undefined,
    };
  },
  componentDidMount() {
    GettingStartedActions.getStatus();
    CurrentUserStore.reload();
  },
  componentDidUpdate() {
    if (!this._isLoading()) {
      this._redirectToStartpage();
    }
  },
  onGettingStartedUpdate(state) {
    this.setState({ gettingStarted: state.status });
  },
  _redirect(page) {
    this.props.history.pushState(null, page);
  },
  _redirectToStartpage() {
    // Show getting started page if user is an admin and getting started wasn't dismissed
    if (PermissionsMixin.isPermitted(this.state.currentUser.permissions, ['inputs:create'])) {
      if (this.state.gettingStarted.show) {
        this._redirect(Routes.GETTING_STARTED);
        return;
      }
    }

    // Show custom startpage if it was set
    const startpage = this.state.currentUser.startpage;
    if (startpage !== null && Object.keys(startpage).length > 0) {
      if (startpage.type === 'stream') {
        this._redirect(Routes.stream_search(startpage.id));
      } else {
        this._redirect(Routes.dashboard_show(startpage.id));
      }
      return;
    }

    // Show search page if permitted, or streams page in other case
    if (PermissionsMixin.isAnyPermitted(this.state.currentUser.permissions, ['searches:absolute', 'searches:keyword', 'searches:relative'])) {
      this._redirect(Routes.SEARCH);
    } else {
      this._redirect(Routes.STREAMS);
    }
  },
  _isLoading() {
    return !this.state.currentUser || !this.state.gettingStarted;
  },
  render() {
    return <Spinner />;
  },
});

export default StartPage;
