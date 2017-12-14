import PropTypes from 'prop-types';
import React from 'react';
import { loadSystemInfo } from 'ducks/system/index';
import { connect } from 'react-redux';

import { DocumentTitle, Spinner } from 'components/common';
import GettingStarted from 'components/gettingstarted/GettingStarted';

import Routes from 'routing/Routes';
import history from 'util/History';

const GETTING_STARTED_URL = 'https://gettingstarted.graylog.org/';
const GettingStartedPage = React.createClass({
  propTypes: {
    location: PropTypes.object.isRequired,
    isLoading: PropTypes.bool,
    system: PropTypes.object,
    loadSystemInfo: PropTypes.func.isRequired,
  },
  componentDidMount() {
    this.props.loadSystemInfo();
  },
  _onDismiss() {
    history.push(Routes.STARTPAGE);
  },
  render() {
    if (this.props.isLoading) {
      return <Spinner />;
    }

    return (
      <DocumentTitle title="Getting started">
        <div>
          <GettingStarted clusterId={this.props.system.cluster_id}
                          masterOs={this.props.system.operating_system}
                          masterVersion={this.props.system.version}
                          gettingStartedUrl={GETTING_STARTED_URL}
                          noDismissButton={Boolean(this.props.location.query.menu)}
                          onDismiss={this._onDismiss} />
        </div>
      </DocumentTitle>
    );
  },
});

const mapStateToProps = state => ({
  isLoading: state.system.frontend.isLoading,
  system: state.system.systemInfo,
});

const mapDispatchToProps = dispatch => ({
  loadSystemInfo: () => dispatch(loadSystemInfo()),
});

export default connect(mapStateToProps, mapDispatchToProps)(GettingStartedPage);
