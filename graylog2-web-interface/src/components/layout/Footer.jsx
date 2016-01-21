import React from 'react';
import Reflux from 'reflux';
import Version from 'util/Version';

import SystemStore from 'stores/system/SystemStore';

const Footer = React.createClass({
  mixins: [Reflux.connect(SystemStore)],
  componentDidMount() {
    SystemStore.jvm().then(jvmInfo => this.setState({jvm: jvmInfo}));
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
