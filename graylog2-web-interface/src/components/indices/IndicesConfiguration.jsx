/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import PropTypes from 'prop-types';
import React from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { Row, Col } from 'components/graylog';
import IndexMaintenanceStrategiesSummary from 'components/indices/IndexMaintenanceStrategiesSummary';
import 'components/indices/rotation'; // Load rotation plugin UI plugins from core.
import 'components/indices/retention'; // Load rotation plugin UI plugins from core.

class IndicesConfiguration extends React.Component {
  static propTypes = {
    indexSet: PropTypes.object.isRequired,
  };

  render() {
    const { indexSet } = this.props;

    if (!indexSet.writable) {
      return (
        <Row>
          <Col md={12}>
            Index set is not writable and will not be included in index rotation and retention.
            It is also not possible to assign it to a stream.
          </Col>
        </Row>
      );
    }

    const rotationConfig = {
      strategy: indexSet.rotation_strategy_class,
      config: indexSet.rotation_strategy,
    };
    const retentionConfig = {
      strategy: indexSet.retention_strategy_class,
      config: indexSet.retention_strategy,
    };

    return (
      <Row>
        <Col md={6}>
          <IndexMaintenanceStrategiesSummary config={rotationConfig}
                                             pluginExports={PluginStore.exports('indexRotationConfig')} />
        </Col>
        <Col md={6}>
          <IndexMaintenanceStrategiesSummary config={retentionConfig}
                                             pluginExports={PluginStore.exports('indexRetentionConfig')} />
        </Col>
      </Row>
    );
  }
}

export default IndicesConfiguration;
