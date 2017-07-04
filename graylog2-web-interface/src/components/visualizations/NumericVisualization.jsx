import React from 'react';
import deepEqual from 'deep-equal';

import NumberUtils from 'util/NumberUtils';

import style from './NumericVisualization.css';

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
    onRenderComplete: React.PropTypes.func,
  },

  getDefaultProps() {
    return {
      onRenderComplete: () => {},
    };
  },

  getInitialState() {
    return {
      currentNumber: undefined,
      previousNumber: undefined,
    };
  },
  componentDidMount() {
    this._updateData(this.props.data, this.props.onRenderComplete);
  },
  componentWillReceiveProps(nextProps) {
    if (deepEqual(this.props, nextProps)) {
      return;
    }
    this._updateData(nextProps.data, this.props.onRenderComplete);
  },

  DEFAULT_VALUE_FONT_SIZE: '70px',
  NUMBER_OF_INDICATORS: 3,
  PERCENTAGE_PER_INDICATOR: 30,

  _updateData(data, renderCallback) {
    const state = this._normalizeStateFromProps(data);
    this.setState(state, renderCallback);
  },

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
    const className = style.trendIcon;

    const indicatorIsActive = this._isIndicatorActive(index, trendIndicatorType);
    if (!indicatorIsActive) {
      return className;
    }

    const lowerClass = this.props.config.lower_is_better ? style.trendGood : style.trendBad;
    const higherClass = this.props.config.lower_is_better ? style.trendBad : style.trendGood;

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
        <div className={style.trendIndicators}>
          <div>
            <div className={this._getHigherIndicatorClass(0)}>
              <span><i className="fa fa-angle-up" /></span>
            </div>
            <div className={this._getHigherIndicatorClass(1)}>
              <span><i className="fa fa-angle-up" /></span>
            </div>
            <div className={this._getHigherIndicatorClass(2)}>
              <span><i className="fa fa-angle-up" /></span>
            </div>
          </div>
          <div>
            <div className={this._getLowerIndicatorClass(0)}>
              <span><i className="fa fa-angle-down" /></span>
            </div>
            <div className={this._getLowerIndicatorClass(1)}>
              <span><i className="fa fa-angle-down" /></span>
            </div>
            <div className={this._getLowerIndicatorClass(2)}>
              <span><i className="fa fa-angle-down" /></span>
            </div>
          </div>
        </div>
      );
    }

    return (
      <div className={style.number}>
        <div className={style.aside} />
        <div className={style.value} style={{ fontSize: this._calculateFontSize() }}>
          {this._formatData()}
        </div>
        <div className={style.aside}>
          {trendIndicators}
        </div>
      </div>
    );
  },
});

export default NumericVisualization;
