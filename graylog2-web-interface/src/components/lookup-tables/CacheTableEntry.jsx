import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import { Button } from 'react-bootstrap';

import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';
import { ContentPackMarker } from 'components/common';
import { MetricsMapper, MetricContainer, CounterRate } from 'components/metrics';
import NumberUtils from 'util/NumberUtils';

const { LookupTableCachesActions } = CombinedProvider.get('LookupTableCaches');

const LUTTableEntry = React.createClass({

  propTypes: {
    cache: React.PropTypes.object.isRequired,
  },

  _onDelete() {
// eslint-disable-next-line no-alert
    if (window.confirm(`Are you sure you want to delete cache "${this.props.cache.title}"?`)) {
      LookupTableCachesActions.delete(this.props.cache.id).then(() => LookupTableCachesActions.reloadPage());
    }
  },

  _onCountMetrics(metrics) {
    let totalHits = 0;
    let totalMisses = 0;

    Object.keys(metrics).map(nodeId => metrics[nodeId].hits.metric.rate.total).forEach((v) => { totalHits += v; });
    Object.keys(metrics).map(nodeId => metrics[nodeId].misses.metric.rate.total).forEach((v) => { totalMisses += v; });

    const total = totalHits + totalMisses;

    if (total < 1) {
      return 'n/a';
    }
    const hitRate = (totalHits * 100.0) / total;
    return `${NumberUtils.formatNumber(hitRate)}%`;
  },

  _onEntriesMetrics(metrics) {
    let total = 0;

    Object.keys(metrics).map(nodeId => metrics[nodeId].count.metric.value.value).forEach((v) => { total += v; });

    if (total < 0) {
      return 'n/a';
    }

    return NumberUtils.formatNumber(total);
  },

  render() {
    const countMap = {
      requests: `org.graylog2.lookup.caches.${this.props.cache.id}.requests`,
      hits: `org.graylog2.lookup.caches.${this.props.cache.id}.hits`,
      misses: `org.graylog2.lookup.caches.${this.props.cache.id}.misses`,
    };
    const entriesMap = {
      count: `org.graylog2.lookup.caches.${this.props.cache.id}.entries`,
    };
    return (
      <tbody>
        <tr>
          <td>
            <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.CACHES.show(this.props.cache.name)}><a>{this.props.cache.title}</a></LinkContainer>
            <ContentPackMarker contentPack={this.props.cache.content_pack} marginLeft={5} />
          </td>
          <td>{this.props.cache.description}</td>
          <td>{this.props.cache.name}</td>
          <td>
            <MetricsMapper map={entriesMap} computeValue={this._onEntriesMetrics} />
          </td>
          <td>
            <MetricsMapper map={countMap} computeValue={this._onCountMetrics} />
          </td>
          <td>
            <MetricContainer name={`org.graylog2.lookup.caches.${this.props.cache.id}.requests`}>
              <CounterRate suffix="lookups/s" />
            </MetricContainer>
          </td>
          <td>
            <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.CACHES.edit(this.props.cache.name)}>
              <Button bsSize="xsmall" bsStyle="info">Edit</Button>
            </LinkContainer>
            &nbsp;
            <Button bsSize="xsmall" bsStyle="primary" onClick={this._onDelete}>Delete</Button>
          </td>
        </tr>
      </tbody>
    );
  },

});

export default LUTTableEntry;

