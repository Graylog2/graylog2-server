import React from 'react';
import Reflux from 'reflux';
import Version from 'util/Version';

import StoreProvider from 'injection/StoreProvider';
const SystemStore = StoreProvider.getStore('System');

const Footer = React.createClass({
  mixins: [Reflux.connect(SystemStore, 'systemStoreState')],
  componentDidMount() {
    SystemStore.jvm().then(jvmInfo => this.setState({ jvm: jvmInfo }));
  },
  _isLoading(system) {
    return !(system && this.state.jvm);
  },
  render() {
    const { system } = this.state.systemStoreState;
    if (this._isLoading(system)) {
      return (
        <div id="footer">
          Graylog {Version.getFullVersion()}
        </div>
      );
    }

    return (
      <div id="footer">
        Graylog {system.version} on {system.hostname} ({this.state.jvm.info})
      </div>
    );
  },
});

export default Footer;
