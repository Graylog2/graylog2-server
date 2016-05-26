import React from 'react';
import { Row, Col } from 'react-bootstrap';

import { PluginStore } from 'graylog-web-plugin/plugin';

const PluginList = React.createClass({
  componentDidMount() {
    this.style.use();
  },

  componentWillUnmount() {
    this.style.unuse();
  },

  style: require('!style/useable!css!./PluginList.css'),

  ENTERPRISE_PLUGINS: {
    ArchivePlugin: 'Archive plugin',
    LicensePlugin: 'License plugin',
  },

  _formatPlugin(pluginName) {
    const plugin = PluginStore.get().filter(plugin => plugin.metadata.name === pluginName)[0];
    return (
      <li key={pluginName} className={plugin ? 'text-success' : 'text-danger'}>
        <i className={`fa fa-${plugin ? 'check-circle' : 'minus-circle'}`}/>&nbsp;
        {this.ENTERPRISE_PLUGINS[pluginName]} is {plugin ? 'installed' : 'not installed'}
      </li>
    );
  },

  render() {
    const enterprisePluginList = Object.keys(this.ENTERPRISE_PLUGINS).map(pluginName => this._formatPlugin(pluginName));

    return (
      <Row className="content">
        <Col md={12}>
          <p>This is the status of Graylog Enterprise modules in this cluster:</p>
          <ul className="enterprise-plugins">
            {enterprisePluginList}
          </ul>
        </Col>
      </Row>
    );
  },
});

export default PluginList;
