import PropTypes from 'prop-types';
import React from 'react';
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { LinkContainer } from 'react-router-bootstrap';
import styled from 'styled-components';

import { Button, ProgressBar } from 'components/graylog';
import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';
import Routes from 'routing/Routes';
import NumberUtils from 'util/NumberUtils';
import { Spinner } from 'components/common';

const MetricsStore = StoreProvider.getStore('Metrics');
const MetricsActions = ActionsProvider.getActions('Metrics');

const NodeBufferUsage = styled.div`
  margin-top: 10px;
  margin-bottom: 7px;
`;

const StyledProgressBar = styled(ProgressBar)`
  margin-bottom: 5px;
`;

const BufferUsage = createReactClass({
  displayName: 'BufferUsage',

  propTypes: {
    bufferType: PropTypes.string.isRequired,
    nodeId: PropTypes.string.isRequired,
    title: PropTypes.node.isRequired,
  },

  mixins: [Reflux.connect(MetricsStore)],

  componentWillMount() {
    const { nodeId } = this.props;
    const prefix = this._metricPrefix();
    const metricNames = [
      `${prefix}.usage`,
      `${prefix}.size`,
    ];
    metricNames.forEach(metricName => MetricsActions.add(nodeId, metricName));
  },

  _metricPrefix() {
    const { bufferType } = this.props;

    return `org.graylog2.buffers.${bufferType}`;
  },

  _metricFilter() {
    const { bufferType } = this.props;

    return `org\\.graylog2\\.buffers\\.${bufferType}\\.|${bufferType}buffer`;
  },

  render() {
    const { metrics } = this.state;

    if (!metrics) {
      return <Spinner />;
    }

    const { nodeId, title } = this.props;
    const prefix = this._metricPrefix();
    const usageMetric = metrics[nodeId][`${prefix}.usage`];
    const usage = usageMetric ? usageMetric.metric.value : NaN;
    const sizeMetric = metrics[nodeId][`${prefix}.size`];
    const size = sizeMetric ? sizeMetric.metric.value : NaN;
    // eslint-disable-next-line no-restricted-globals
    const usagePercentage = ((!isNaN(usage) && !isNaN(size)) ? usage / size : 0);
    const percentLabel = NumberUtils.formatPercentage(usagePercentage);

    return (
      <div>
        <LinkContainer to={Routes.filtered_metrics(nodeId, this._metricFilter())}>
          <Button bsSize="xsmall" className="pull-right">Metrics</Button>
        </LinkContainer>
        <h3>{title}</h3>
        <NodeBufferUsage>
          <StyledProgressBar bars={[{
            value: usagePercentage * 100,
            bsStyle: 'warning',
            label: percentLabel,
          }]} />
        </NodeBufferUsage>
        <span><strong>{usage} messages</strong> in {title.toLowerCase()}, {percentLabel} utilized.</span>
      </div>
    );
  },
});

export default BufferUsage;
