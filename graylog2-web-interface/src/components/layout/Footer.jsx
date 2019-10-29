import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import Version from 'util/Version';

import StoreProvider from 'injection/StoreProvider';

const SystemStore = StoreProvider.getStore('System');

const Footer = createReactClass({
  displayName: 'Footer',
  mixins: [Reflux.connect(SystemStore)],
  mounted: false,

  componentDidMount() {
    this.mounted = true;
    SystemStore.jvm().then((jvmInfo) => {
      if (this.mounted) {
        this.setState({ jvm: jvmInfo });
      }
    });
  },

  componentWillUnmount() {
    this.mounted = false;
  },

  _isLoading() {
    return !(this.state.system && this.state.jvm);
  },

  render() {
    if (this._isLoading()) {
      return (
        <div id="footer">
          Graylog {Version.getFullVersion()}
        </div>
      );
    }

    return (
      <div id="footer">
        Graylog {this.state.system.version} on {this.state.system.hostname} ({this.state.jvm.info})
      </div>
    );
  },
});

export default Footer;
