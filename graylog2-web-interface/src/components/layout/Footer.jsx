import React from 'react';
import PropTypes from 'prop-types';
import { inject, observer } from 'mobx-react';
import Version from 'util/Version';

const Footer = React.createClass({
  propTypes: {
    isLoading: PropTypes.bool.isRequired,
    systemInfo: PropTypes.object,
    jvmInfo: PropTypes.object,
    getJvmInfo: PropTypes.func.isRequired,
  },

  componentDidMount() {
    this.props.getJvmInfo();
  },
  render() {
    const { isLoading, systemInfo, jvmInfo } = this.props;
    if (isLoading) {
      return (
        <div id="footer">
          Graylog {Version.getFullVersion()}
        </div>
      );
    }

    return (
      <div id="footer">
        Graylog {systemInfo.version} on {systemInfo.hostname} ({jvmInfo.info})
      </div>
    );
  },
});

export default inject(context => ({
  isLoading: context.rootStore.systemInfoStore.isLoading || context.rootStore.jvmInfoStore.isLoading,
  systemInfo: context.rootStore.systemInfoStore.systemInfo,
  jvmInfo: context.rootStore.jvmInfoStore.jvmInfo,
  getJvmInfo: () => context.rootStore.jvmInfoStore.getJvmInfo(),
}))(observer(Footer));
