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
import numeral from 'numeral';
import moment from 'moment';

class ShardMeter extends React.Component {
  static propTypes = {
    title: PropTypes.string.isRequired,
    shardMeter: PropTypes.object.isRequired,
  };

  _formatMeter = (meter) => {
    const value = <span>{numeral(meter.total).format('0,0')} ops</span>;

    if (meter.total > 0) {
      return <span>{value} <span title={`${meter.time_seconds}s`}>(took {moment.duration(meter.time_seconds, 'seconds').humanize()})</span></span>;
    }

    return value;
  };

  render() {
    const sm = this.props.shardMeter;

    return (
      <span>
        <h3 style={{ display: 'inline' }}>{this.props.title}</h3>
        <dl>
          <dt>Index:</dt>
          <dd>{this._formatMeter(sm.index)}</dd>

          <dt>Flush:</dt>
          <dd>{this._formatMeter(sm.flush)}</dd>

          <dt>Merge:</dt>
          <dd>{this._formatMeter(sm.merge)}</dd>

          <dt>Query:</dt>
          <dd>{this._formatMeter(sm.search_query)}</dd>

          <dt>Fetch:</dt>
          <dd>{this._formatMeter(sm.search_fetch)}</dd>

          <dt>Get:</dt>
          <dd>{this._formatMeter(sm.get)}</dd>

          <dt>Refresh:</dt>
          <dd>{this._formatMeter(sm.refresh)}</dd>
        </dl>
      </span>
    );
  }
}

export default ShardMeter;
