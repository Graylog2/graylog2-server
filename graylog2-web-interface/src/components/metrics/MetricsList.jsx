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
import styled, { css } from 'styled-components';

import { Metric } from 'components/metrics';

const MetricListWrap = styled.ul(({ theme }) => css`
  padding: 0;

  li {
    margin-bottom: 5px;

    .prefix {
      color: ${theme.colors.gray[70]};
    }

    .name {
      font-size: ${theme.fonts.size.body};
      font-family: ${theme.fonts.family.monospace};
      word-break: break-all;

      .open:hover {
        text-decoration: none;
      }
    }

    .metric {
      margin-left: 10px;
      padding: 10px;

      h3 {
        margin-bottom: 5px;
      }
    }
  }

  dl {
    margin-top: 0;
    margin-bottom: 0;
  }
`);

class MetricsList extends React.Component {
  static propTypes = {
    names: PropTypes.arrayOf(PropTypes.object).isRequired,
    namespace: PropTypes.string.isRequired,
    nodeId: PropTypes.string.isRequired,
  };

  _formatMetric = (metric) => {
    const { namespace, nodeId } = this.props;

    return (
      <li key={`li-${metric.full_name}`}>
        <Metric key={metric.full_name} metric={metric} namespace={namespace} nodeId={nodeId} />
      </li>
    );
  };

  render() {
    const { names } = this.props;
    const metrics = names
      .sort((m1, m2) => m1.full_name.localeCompare(m2.full_name))
      .map((metric) => this._formatMetric(metric));

    return (
      <MetricListWrap>
        {metrics.length > 0 ? metrics : <li>No metrics match the given filter. Please ensure you use a valid regular expression</li>}
      </MetricListWrap>
    );
  }
}

export default MetricsList;
