import React from 'react';
import deepEqual from 'deep-equal';

import NumberUtils from 'util/NumberUtils';

const TrendIndicatorType = {
  HIGHER: 'higher',
  LOWER: 'lower',
};

const NumericVisualization = React.createClass({
  propTypes: {
    config: React.PropTypes.object.isRequired,
    data: React.PropTypes.oneOfType([
      React.PropTypes.object,
      React.PropTypes.number,
    ]).isRequired,
  },
  getInitialState() {
    return {
      currentNumber: undefined,
      previousNumber: undefined,
    };
  },
  componentDidMount() {
    const state = this._normalizeStateFromProps(this.props.data);
    this.setState(state);
  },
  componentWillReceiveProps(nextProps) {
    if (deepEqual(this.props, nextProps)) {
      return;
    }

    const state = this._normalizeStateFromProps(nextProps.data);
    this.setState(state);
  },
  DEFAULT_VALUE_FONT_SIZE: '70px',
  NUMBER_OF_INDICATORS: 3,
  PERCENTAGE_PER_INDICATOR: 30,
  _normalizeStateFromProps(props) {
    let state = {};
    if (typeof props === 'object') {
      const normalizedNowNumber = NumberUtils.normalizeNumber(props.now);
      const normalizedPreviousNumber = NumberUtils.normalizeNumber(props.previous);
      state = {
        currentNumber: normalizedNowNumber,
        previousNumber: normalizedPreviousNumber,
        percentage: this._calculatePercentage(normalizedNowNumber, normalizedPreviousNumber),
      };
    } else {
      state = { currentNumber: props };
    }
    return state;
  },
  _calculatePercentage(nowNumber, previousNumber) {
    let percentage;
    if (previousNumber === 0 || isNaN(previousNumber)) {
      let factor = 0;
      if (nowNumber > previousNumber) {
        factor = 1;
      } else if (nowNumber < previousNumber) {
        factor = -1;
      }

      percentage = 100 * factor;
    } else {
      percentage = ((nowNumber - previousNumber) / previousNumber) * 100;
    }

    return percentage;
  },
  _calculateFontSize() {
    if (typeof this.props.data === 'undefined') {
      return this.DEFAULT_VALUE_FONT_SIZE;
    }

    let fontSize;
    const numberOfDigits = this._formatData().replace(/[,.]/g, '').length;

    if (numberOfDigits < 7) {
      fontSize = this.DEFAULT_VALUE_FONT_SIZE;
    } else {
      switch (numberOfDigits) {
        case 7:
          fontSize = '60px';
          break;
        case 8:
          fontSize = '50px';
          break;
        case 9:
        case 10:
          fontSize = '40px';
          break;
        case 11:
        case 12:
          fontSize = '35px';
          break;
        default:
          fontSize = '25px';
      }
    }

    return fontSize;
  },
  _formatData() {
    return String(NumberUtils.formatNumber(this.state.currentNumber));
  },
  _isIndicatorActive(index, trendIndicatorType) {
    if ((this.state.percentage === 0) ||
      (this.state.currentNumber >= this.state.previousNumber && trendIndicatorType !== TrendIndicatorType.HIGHER) ||
      (this.state.currentNumber <= this.state.previousNumber && trendIndicatorType !== TrendIndicatorType.LOWER)) {
      return false;
    }

    const reverseIndex = trendIndicatorType === TrendIndicatorType.HIGHER;

    if (reverseIndex) {
      index = Math.abs(index - (this.NUMBER_OF_INDICATORS - 1));
    }
    return Math.abs(this.state.percentage) >= this.PERCENTAGE_PER_INDICATOR * index;
  },
  _getIndicatorClass(index, trendIndicatorType) {
    const className = 'trend-icon';

    const indicatorIsActive = this._isIndicatorActive(index, trendIndicatorType);
    if (!indicatorIsActive) {
      return className;
    }

    const lowerClass = this.props.config.lower_is_better ? 'trend-good' : 'trend-bad';
    const higherClass = this.props.config.lower_is_better ? 'trend-bad' : 'trend-good';

    const activeClass = trendIndicatorType === TrendIndicatorType.HIGHER ? higherClass : lowerClass;

    return `${className} ${activeClass}`;
  },
  _getHigherIndicatorClass(index) {
    return this._getIndicatorClass(index, TrendIndicatorType.HIGHER);
  },
  _getLowerIndicatorClass(index) {
    return this._getIndicatorClass(index, TrendIndicatorType.LOWER);
  },
  render() {
    let trendIndicators;

    if (this.props.config.trend) {
      trendIndicators = (
        <div className="trend-indicators">
          <div className="trend-icons-higher">
            <div className={this._getHigherIndicatorClass(0)}>
              <span className="trend-higher"><i className="fa fa-angle-up" /></span>
            </div>
            <div className={this._getHigherIndicatorClass(1)}>
              <span className="trend-higher"><i className="fa fa-angle-up" /></span>
            </div>
            <div className={this._getHigherIndicatorClass(2)}>
              <span className="trend-higher"><i className="fa fa-angle-up" /></span>
            </div>
          </div>
          <div className="trend-icons-lower">
            <div className={this._getLowerIndicatorClass(0)}>
              <span className="trend-lower"><i className="fa fa-angle-down" /></span>
            </div>
            <div className={this._getLowerIndicatorClass(1)}>
              <span className="trend-lower"><i className="fa fa-angle-down" /></span>
            </div>
            <div className={this._getLowerIndicatorClass(2)}>
              <span className="trend-lower"><i className="fa fa-angle-down" /></span>
            </div>
          </div>
        </div>
      );
    }

    return (
      <div className="number">
        <div className="text-center">
          <span className="value" style={{ fontSize: this._calculateFontSize() }}>
            {this._formatData()}
          </span>
          {trendIndicators}
        </div>
      </div>
    );
  },
});

export default NumericVisualization;
