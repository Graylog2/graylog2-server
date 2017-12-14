import React from 'react';
import { PropTypes } from 'prop-types';
import { connect } from 'react-redux';

import Version from 'util/Version';
import { loadSystemInfo, loadJvmInfo } from 'ducks/system/index';

const Footer = React.createClass({
  propTypes: {
    isLoading: PropTypes.bool.isRequired,
    system: PropTypes.object,
    jvm: PropTypes.object,
  },

  componentDidMount() {
    this.loadData();
  },

  loadData() {
    this.props.loadSystemInfo();
    this.props.loadJvmInfo();
  },

  render() {
    if (this.props.isLoading) {
      return (
        <div id="footer">
          Graylog {Version.getFullVersion()}
        </div>
      );
    }

    return (
      <div id="footer">
        Graylog {this.props.system.version} on {this.props.system.hostname} ({this.props.jvm.info})
      </div>
    );
  },
});

const mapStateToProps = state => ({
  isLoading: state.system.system.frontend.isLoading || state.system.jvm.frontend.isLoading,
  system: state.system.system.systemInfo,
  jvm: state.system.jvm.jvmInfo,
});

const mapDispatchToProps = dispatch => ({
  loadSystemInfo: () => dispatch(loadSystemInfo()),
  loadJvmInfo: () => dispatch(loadJvmInfo()),
});

export default connect(mapStateToProps, mapDispatchToProps)(Footer);
