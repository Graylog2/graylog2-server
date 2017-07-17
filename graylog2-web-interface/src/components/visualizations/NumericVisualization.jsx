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
      onRenderComplete: () => {
      },
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

  DEFAULT_VALUE_FONT_SIZE: '60px',
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
    const formattedLength = this._formatData().length;

    if (formattedLength < 7) {
      fontSize = this.DEFAULT_VALUE_FONT_SIZE;
    } else {
      switch (formattedLength) {
        case 7:
          fontSize = '50px';
          break;
        case 8:
          fontSize = '45px';
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
        <g transform="translate(270,45)">
          <g transform="translate(0,-17)">
            <path d="M0 5 L5 0 L10 5" className={this._getHigherIndicatorClass(0)} />
            <path d="M0 10 L5 5 L10 10" className={this._getHigherIndicatorClass(1)} />
            <path d="M0 15 L5 10 L10 15" className={this._getHigherIndicatorClass(2)} />
          </g>
          <g transform="translate(0, 2) rotate(180,5,7.5)">
            <path d="M0 5 L5 0 L10 5" className={this._getLowerIndicatorClass(2)} />
            <path d="M0 10 L5 5 L10 10" className={this._getLowerIndicatorClass(1)} />
            <path d="M0 15 L5 10 L10 15" className={this._getLowerIndicatorClass(0)} />
          </g>
        </g>
      );
    }

    return (
      <div className={style.container}>
        <svg viewBox="0 0 300 100" className={style.number}>
          <text x="150" y="45" className={style.value} style={{ fontSize: this._calculateFontSize() }}>
            {this._formatData()}
          </text>
          {trendIndicators}
        </svg>
      </div>
    );
  },
});

export default NumericVisualization;
