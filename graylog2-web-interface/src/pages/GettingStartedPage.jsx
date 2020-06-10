import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { DocumentTitle, Spinner } from 'components/common';
import GettingStarted from 'components/gettingstarted/GettingStarted';

import Routes from 'routing/Routes';
import history from 'util/History';

import StoreProvider from 'injection/StoreProvider';

const SystemStore = StoreProvider.getStore('System');

const GETTING_STARTED_URL = 'https://gettingstarted.graylog.org/';
const GettingStartedPage = createReactClass({
  displayName: 'GettingStartedPage',

  propTypes: {
    location: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(SystemStore)],

  _isLoading() {
    return !this.state.system;
  },

  _onDismiss() {
    history.push(Routes.STARTPAGE);
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    return (
      <DocumentTitle title="Getting started">
        <div>
          <GettingStarted clusterId={this.state.system.cluster_id}
                          primaryOs={this.state.system.operating_system}
                          primaryVersion={this.state.system.version}
                          gettingStartedUrl={GETTING_STARTED_URL}
                          noDismissButton={Boolean(this.props.location.query.menu)}
                          onDismiss={this._onDismiss} />
        </div>
      </DocumentTitle>
    );
  },
});

export default GettingStartedPage;
