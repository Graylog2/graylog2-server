import PropTypes from 'prop-types';
import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';
import { PluginStore } from 'graylog-web-plugin/plugin';

import Routes from 'routing/Routes';
import { Row, Col, Button } from 'components/graylog';
import { ContentPackMarker } from 'components/common';

import Styles from './ConfigSummary.css';

const Cache = ({ cache }) => {
  const plugins = {};
  PluginStore.exports('lookupTableCaches').forEach((p) => {
    plugins[p.type] = p;
  });

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
        <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.CACHES.edit(cache.name)}>
          <Button bsStyle="success">Edit</Button>
        </LinkContainer>
      </Col>
      <Col md={6} />
    </Row>
  );
};

Cache.propTypes = {
  cache: PropTypes.object.isRequired,
};

export default Cache;
