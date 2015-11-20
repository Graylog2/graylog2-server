import React from 'react';
import Reflux from 'reflux';
import GettingStarted from 'components/gettingstarted/GettingStarted';

import {Spinner} from 'components/common';

import SystemStore from 'stores/system/SystemStore';

const GETTING_STARTED_URL = 'https://versioncheck.graylog.com/getting-started';
const GettingStartedPage = React.createClass({
  mixins: [Reflux.connect(SystemStore)],
  _isLoading() {
    return !this.state.system;
  },
  render() {
    if (this._isLoading()) {
      return <Spinner/>;
    }

    return (
      <div>
        <GettingStarted clusterId={this.state.system.cluster_id}
                        masterOs={this.state.system.operating_system}
                        masterVersion={this.state.system.version}
                        gettingStartedUrl={GETTING_STARTED_URL}/>
      </div>
    );
  },
});

export default GettingStartedPage;
