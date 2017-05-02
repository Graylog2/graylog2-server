import React from 'react';
import { Row, Col } from 'react-bootstrap';
import { PluginStore } from 'graylog-web-plugin/plugin';

import Styles from './ConfigSummary.css';

const DataAdapter = React.createClass({

  propTypes: {
    dataAdapter: React.PropTypes.object.isRequired,
  },

  render() {
    const plugins = {};
    PluginStore.exports('lookupTableAdapters').forEach((p) => {
      plugins[p.type] = p;
    });

    const dataAdapter = this.props.dataAdapter;
    const plugin = plugins[dataAdapter.config.type];
    if (!plugin) {
      return <p>Unknown data adapter type {dataAdapter.config.type}. Is the plugin missing?</p>;
    }

    const summary = plugin.summaryComponent;
    return (
      <Row className="content">
        <Col md={6}>
          <h2>{dataAdapter.title} <small>({plugin.displayName})</small></h2>
          <div className={Styles.config}>
            <dl>
              <dt>Description</dt>
              <dd>{dataAdapter.description || <em>No description.</em>}</dd>
            </dl>
          </div>
          <h4>Configuration</h4>
          <div className={Styles.config}>
            {React.createElement(summary, { dataAdapter: dataAdapter })}
          </div>
        </Col>
        <Col md={6}>
          <h3>Retrieve data</h3>
          <p>Use this to manually trigger the data adapter.</p>
        </Col>
      </Row>
    );
  },

});

export default DataAdapter;
