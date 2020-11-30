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

import { LinkContainer, Link } from 'components/graylog/router';
import { Button } from 'components/graylog';
import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';
import { ContentPackMarker } from 'components/common';
import { MetricsMapper, MetricContainer, CounterRate } from 'components/metrics';
import NumberUtils from 'util/NumberUtils';

const { LookupTableCachesActions } = CombinedProvider.get('LookupTableCaches');

class LUTTableEntry extends React.Component {
  static propTypes = {
    cache: PropTypes.object.isRequired,
  };

  _onDelete = () => {
    const { cache } = this.props;

    // eslint-disable-next-line no-alert
    if (window.confirm(`Are you sure you want to delete cache "${cache.title}"?`)) {
      LookupTableCachesActions.delete(cache.id).then(() => LookupTableCachesActions.reloadPage());
    }
  };

  _onCountMetrics = (metrics) => {
    let totalHits = 0;
    let totalMisses = 0;

    Object.keys(metrics).map((nodeId) => metrics[nodeId].hits.metric.rate.total).forEach((v) => { totalHits += v; });
    Object.keys(metrics).map((nodeId) => metrics[nodeId].misses.metric.rate.total).forEach((v) => { totalMisses += v; });

    const total = totalHits + totalMisses;

    if (total < 1) {
      return 'n/a';
    }

    const hitRate = (totalHits * 100.0) / total;

    return `${NumberUtils.formatNumber(hitRate)}%`;
  };

  _onEntriesMetrics = (metrics) => {
    let total = 0;

    Object.keys(metrics).map((nodeId) => metrics[nodeId].count.metric.value).forEach((v) => { total += v; });

    if (total < 0) {
      return 'n/a';
    }

    return NumberUtils.formatNumber(total);
  };

  render() {
    const { cache } = this.props;

    const countMap = {
      requests: `org.graylog2.lookup.caches.${cache.id}.requests`,
      hits: `org.graylog2.lookup.caches.${cache.id}.hits`,
      misses: `org.graylog2.lookup.caches.${cache.id}.misses`,
    };
    const entriesMap = {
      count: `org.graylog2.lookup.caches.${cache.id}.entries`,
    };

    return (
      <tbody>
        <tr>
          <td>
            <Link to={Routes.SYSTEM.LOOKUPTABLES.CACHES.show(cache.name)}>{cache.title}</Link>
            <ContentPackMarker contentPack={cache.content_pack} marginLeft={5} />
          </td>
          <td>{cache.description}</td>
          <td>{cache.name}</td>
          <td>
            <MetricsMapper map={entriesMap} computeValue={this._onEntriesMetrics} />
          </td>
          <td>
            <MetricsMapper map={countMap} computeValue={this._onCountMetrics} />
          </td>
          <td>
            <MetricContainer name={`org.graylog2.lookup.caches.${cache.id}.requests`}>
              <CounterRate suffix="lookups/s" />
            </MetricContainer>
          </td>
          <td>
            <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.CACHES.edit(cache.name)}>
              <Button bsSize="xsmall" bsStyle="info">Edit</Button>
            </LinkContainer>
            &nbsp;
            <Button bsSize="xsmall" bsStyle="primary" onClick={this._onDelete}>Delete</Button>
          </td>
        </tr>
      </tbody>
    );
  }
}

export default LUTTableEntry;
