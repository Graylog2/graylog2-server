import PropTypes from 'prop-types';
import React from 'react';
import { observer } from 'mobx-react';

import { DocumentTitle, Spinner } from 'components/common';
import GettingStarted from 'components/gettingstarted/GettingStarted';
import RootStore from 'stores/RootStore';

import Routes from 'routing/Routes';
import history from 'util/History';

const GETTING_STARTED_URL = 'https://gettingstarted.graylog.org/';
const GettingStartedPage = React.createClass({
  propTypes: {
    location: PropTypes.object.isRequired,
  },
  _onDismiss() {
    history.push(Routes.STARTPAGE);
  },
  render() {
    if (RootStore.systemInfoStore.isLoading) {
      return <Spinner />;
    }

    return (
      <DocumentTitle title="Getting started">
        <div>
          <GettingStarted clusterId={RootStore.systemInfoStore.systemInfo.cluster_id}
                          masterOs={RootStore.systemInfoStore.systemInfo.operating_system}
                          masterVersion={RootStore.systemInfoStore.systemInfo.version}
                          gettingStartedUrl={GETTING_STARTED_URL}
                          noDismissButton={Boolean(this.props.location.query.menu)}
                          onDismiss={this._onDismiss} />
        </div>
      </DocumentTitle>
    );
  },
});

export default observer(GettingStartedPage);
