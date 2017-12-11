import React from 'react';
import { PropTypes } from 'prop-types';
import Version from 'util/Version';
import { loadSystemInfo } from 'ducks/system';
import { loadJvmInfo } from 'ducks/jvm';
import createContainer from 'components/createContainer';

const Footer = React.createClass({
  propTypes: {
    isLoading: PropTypes.bool.isRequired,
    system: PropTypes.object,
    jvm: PropTypes.object,
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
  isLoading: state.system.frontend.isLoading || state.jvm.frontend.isLoading,
  system: state.system.systemInfo,
  jvm: state.jvm.jvmInfo,
});

const mapDispatchToProps = dispatch => ({
  loadSystemInfo: () => dispatch(loadSystemInfo()),
  loadJvmInfo: () => dispatch(loadJvmInfo()),
});

export default createContainer(mapStateToProps, mapDispatchToProps)(Footer, {
  componentWillMount: ['loadSystemInfo', 'loadJvmInfo'],
});
