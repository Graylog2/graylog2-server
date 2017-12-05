import PropTypes from 'prop-types';
import React from 'react';
import Reflux from 'reflux';

import { DocumentTitle, Spinner } from 'components/common';
import GettingStarted from 'components/gettingstarted/GettingStarted';

import Routes from 'routing/Routes';
import history from 'util/History';

import StoreProvider from 'injection/StoreProvider';

const SystemStore = StoreProvider.getStore('System');

const GETTING_STARTED_URL = 'https://gettingstarted.graylog.org/';
const GettingStartedPage = React.createClass({
  propTypes: {
    location: PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(SystemStore, 'system')],
  _isLoading() {
    return !this.state.system.system;
  },
  _onDismiss() {
    history.push(Routes.STARTPAGE);
  },
  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const { system } = this.state.system;
    return (
      <DocumentTitle title="Getting started">
        <div>
          <GettingStarted clusterId={system.cluster_id}
                          masterOs={system.operating_system}
                          masterVersion={system.version}
                          gettingStartedUrl={GETTING_STARTED_URL}
                          noDismissButton={Boolean(this.props.location.query.menu)}
                          onDismiss={this._onDismiss} />
        </div>
      </DocumentTitle>
    );
  },
});

export default GettingStartedPage;
