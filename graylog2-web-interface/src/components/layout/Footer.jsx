import React from 'react';
import PropTypes from 'prop-types';
import { inject, observer } from 'mobx-react';
import Version from 'util/Version';
import StoreProvider from 'injection/StoreProvider';

const JvmInfoStore = StoreProvider.getStore('JvmInfo');
const SystemInfoStore = StoreProvider.getStore('SystemInfo');

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

export default inject(() => ({
  isLoading: SystemInfoStore.isLoading || JvmInfoStore.isLoading,
  systemInfo: SystemInfoStore.systemInfo,
  jvmInfo: JvmInfoStore.jvmInfo,
  getJvmInfo: () => JvmInfoStore.getJvmInfo(),
}))(observer(Footer));
