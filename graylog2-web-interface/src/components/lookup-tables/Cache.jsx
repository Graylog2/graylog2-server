import React from 'react';
import { Row, Col } from 'react-bootstrap';
import { PluginStore } from 'graylog-web-plugin/plugin';
import { ContentPackMarker } from 'components/common';

import Styles from './ConfigSummary.css';

const Cache = React.createClass({

  propTypes: {
    cache: React.PropTypes.object.isRequired,
  },

  render() {
    const plugins = {};
    PluginStore.exports('lookupTableCaches').forEach((p) => {
      plugins[p.type] = p;
    });

    const cache = this.props.cache;
    const plugin = plugins[cache.config.type];
    if (!plugin) {
      return <p>Unknown cache type {cache.config.type}. Is the plugin missing?</p>;
    }

    const summary = plugin.summaryComponent;
    return (
      <Row className="content">
        <Col md={6}>
          <h2>
            {cache.title}
            <ContentPackMarker contentPack={cache.content_pack} marginLeft={5} />
            {' '}
            <small>({plugin.displayName})</small>
          </h2>
          <div className={Styles.config}>
            <dl>
              <dt>Description</dt>
              <dd>{cache.description || <em>No description.</em>}</dd>
            </dl>
          </div>
          <h4>Configuration</h4>
          <div className={Styles.config}>
            {React.createElement(summary, { cache: cache })}
          </div>
        </Col>
        <Col md={6}>
          <h3>TODO: Cached data</h3>
          <p>Use this to inspect the lookup table cache.</p>
        </Col>
      </Row>
    );
  },

});

export default Cache;
