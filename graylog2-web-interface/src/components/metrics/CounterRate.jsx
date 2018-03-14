import PropTypes from 'prop-types';
import React from 'react';
import numeral from 'numeral';
import TimeHelper from 'util/TimeHelper';

class CounterRate extends React.Component {
  static propTypes = {
    metric: PropTypes.object,
    showTotal: PropTypes.bool,
    prefix: PropTypes.string,
    suffix: PropTypes.string,
    hideOnZero: PropTypes.bool,
    hideOnMissing: PropTypes.bool,
  };

  static defaultProps = {
    showTotal: false,
    prefix: null,
    suffix: 'per second',
    hideOnZero: false,
    hideOnMissing: false,
  };

  state = {
    prevMetric: null,
    prevTs: null,
    nowTs: TimeHelper.nowInSeconds(),
  };

  componentWillReceiveProps() {
    this.setState({
      prevMetric: this.props.metric,
      prevTs: this.state.nowTs,
      nowTs: TimeHelper.nowInSeconds(),
    });
  }

  _checkPrevMetric = () => {
    return this.state.prevMetric && this.state.prevMetric.count !== undefined && this.state.prevTs;
  };

  _placeholder = () => {
    if (this.props.hideOnZero) {
      return null;
    }
    return (<span>{this._prefix()}Calculating...</span>);
  };

  _prefix = () => {
    if (this.props.prefix) {
      return `${this.props.prefix} `;
    }
    return null;
  };

  _suffix = () => {
    if (this.props.suffix) {
      return ` ${this.props.suffix}`;
    }
    return null;
  };

  render() {
    if (!(this.props.metric && this.props.metric.count !== undefined)) {
      if (this.props.hideOnMissing) {
        return null;
      }
      if (!this._checkPrevMetric()) {
        return this._placeholder();
      }
    }
    const count = this.props.metric.count;

    let rate = null;
    if (this._checkPrevMetric()) {
      const rateNum = (count - this.state.prevMetric.count) / (this.state.nowTs - this.state.prevTs);
      rate = (<span key="rate" className="number-format">{this._prefix()}{numeral(rateNum).format('0,0')}{this._suffix()}</span>);
    } else {
      return this._placeholder();
    }

    if (this.props.hideOnMissing && !rate) {
      return null;
    }

    return (<span>
      {rate}
      {this.props.showTotal && <span key="absolute" className="number-format"> ({numeral(count).format('0')} total)</span>}
    </span>);
  }
}

export default CounterRate;
