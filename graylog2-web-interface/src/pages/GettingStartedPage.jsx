import PropTypes from 'prop-types';
import React from 'react';
import { inject, observer } from 'mobx-react';

import { DocumentTitle, Spinner } from 'components/common';
import GettingStarted from 'components/gettingstarted/GettingStarted';

import Routes from 'routing/Routes';
import history from 'util/History';

const GETTING_STARTED_URL = 'https://gettingstarted.graylog.org/';
const GettingStartedPage = React.createClass({
  propTypes: {
    isLoading: PropTypes.bool.isRequired,
    systemInfo: PropTypes.object,
    location: PropTypes.object.isRequired,
  },
  _onDismiss() {
    history.push(Routes.STARTPAGE);
  },
  render() {
    const { isLoading, systemInfo } = this.props;
    if (isLoading) {
      return <Spinner />;
    }

    return (
      <DocumentTitle title="Getting started">
        <div>
          <GettingStarted clusterId={systemInfo.cluster_id}
                          masterOs={systemInfo.operating_system}
                          masterVersion={systemInfo.version}
                          gettingStartedUrl={GETTING_STARTED_URL}
                          noDismissButton={Boolean(this.props.location.query.menu)}
                          onDismiss={this._onDismiss} />
        </div>
      </DocumentTitle>
    );
  },
});

export default inject(context => ({
  isLoading: context.rootStore.systemInfoStore.isLoading,
  systemInfo: context.rootStore.systemInfoStore.systemInfo,
}))(observer(GettingStartedPage));
