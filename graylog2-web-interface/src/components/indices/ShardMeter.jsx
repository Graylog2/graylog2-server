import React from 'react';
import numeral from 'numeral';
import moment from 'moment';

const ShardMeter = React.createClass({
  propTypes: {
    title: React.PropTypes.string.isRequired,
    shardMeter: React.PropTypes.object.isRequired,
  },
  _formatMeter(meter) {
    const value = <span>{numeral(meter.total).format('0,0')} ops</span>;

    if (meter.total > 0) {
      return <span>{value} <span title={`${meter.time_seconds}s`}>(took {moment.duration(meter.time_seconds, 'seconds').humanize()})</span></span>;
    }

    return value;
  },
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
  },
});

export default ShardMeter;
