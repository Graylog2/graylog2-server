import React from 'react';
import { Row, Col } from 'react-bootstrap';

import { PluginStore } from 'graylog-web-plugin/plugin';

const PluginList = React.createClass({
  ENTERPRISE_PLUGINS: {
    'ArchivePlugin': 'Archive plugin',
  },

  _formatPlugin(pluginName) {
    const plugin = PluginStore.get().filter(plugin => plugin.metadata.name === pluginName)[0];
    return (
      <li className={plugin ? 'text-success' : 'text-danger'}>
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
          <p>The following Graylog Enterprise modules are active in this cluster:</p>
          <ul className="enterprise-plugins">
            {enterprisePluginList}
          </ul>
        </Col>
      </Row>
    );
  },
});

export default PluginList;
