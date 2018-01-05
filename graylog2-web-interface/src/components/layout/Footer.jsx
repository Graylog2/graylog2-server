import React from 'react';
import { observer } from 'mobx-react';
import Version from 'util/Version';

import RootStore from 'stores/RootStore';

const Footer = React.createClass({
  componentDidMount() {
    RootStore.jvmInfoStore.getJvmInfo();
  },
  render() {
    if (RootStore.systemInfoStore.isLoading || RootStore.jvmInfoStore.isLoading) {
      return (
        <div id="footer">
          Graylog {Version.getFullVersion()}
        </div>
      );
    }

    return (
      <div id="footer">
        Graylog {RootStore.systemInfoStore.systemInfo.version} on {RootStore.systemInfoStore.systemInfo.hostname} ({RootStore.jvmInfoStore.jvmInfo.info})
      </div>
    );
  },
});

export default observer(Footer);
