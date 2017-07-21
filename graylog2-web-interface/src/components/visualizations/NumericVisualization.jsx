import React from 'react';
import deepEqual from 'deep-equal';

import NumberUtils from 'util/NumberUtils';

import style from './NumericVisualization.css';

const TrendIndicatorType = {
  HIGHER: 'higher',
  LOWER: 'lower',
};

const TREND_ICON_COLOR = '#E3E5E5';
const TREND_ICON_GOOD_COLOR = '#8DC63F';
const TREND_ICON_BAD_COLOR = '#BE1E2D';

const NumericVisualization = React.createClass({
  propTypes: {
    config: React.PropTypes.object.isRequired,
    data: React.PropTypes.oneOfType([
      React.PropTypes.object,
      React.PropTypes.number,
    ]).isRequired,
    height: React.PropTypes.number,
    width: React.PropTypes.number,
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
  _getStrokeColor(index, trendIndicatorType) {
    const indicatorIsActive = this._isIndicatorActive(index, trendIndicatorType);
    if (!indicatorIsActive) {
      return TREND_ICON_COLOR;
    }

    const lowerStroke = this.props.config.lower_is_better ? TREND_ICON_GOOD_COLOR : TREND_ICON_BAD_COLOR;
    const higherStroke = this.props.config.lower_is_better ? TREND_ICON_BAD_COLOR : TREND_ICON_GOOD_COLOR;

    const activeStroke = trendIndicatorType === TrendIndicatorType.HIGHER ? higherStroke : lowerStroke;

    return activeStroke;
  },
  _getHigherStrokeColor(index) {
    return this._getStrokeColor(index, TrendIndicatorType.HIGHER);
  },
  _getLowerStrokeColor(index) {
    return this._getStrokeColor(index, TrendIndicatorType.LOWER);
  },
  // We need to set some attributes in the DOM elements that React v0.14 does not support.
  // This is a hack to workaround it as suggested in https://github.com/facebook/react/pull/5210
  _setAttribute(attribute, value) {
    return (node) => {
      if (node) {
        node.setAttribute(attribute, value);
      }
    };
  },
  render() {
    const { config, width, height } = this.props;

    let trendIndicators;

    if (config.trend) {
      trendIndicators = (
        <g transform="translate(270,45)">
          <g transform="translate(0,-17)">
            <path d="M0 5 L5 0 L10 5" fill="none" stroke={this._getHigherStrokeColor(0)} />
            <path d="M0 10 L5 5 L10 10" fill="none" stroke={this._getHigherStrokeColor(1)} />
            <path d="M0 15 L5 10 L10 15" fill="none" stroke={this._getHigherStrokeColor(2)} />
          </g>
          <g transform="translate(0, 2) rotate(180,5,7.5)">
            <path d="M0 5 L5 0 L10 5" fill="none" stroke={this._getLowerStrokeColor(2)} />
            <path d="M0 10 L5 5 L10 10" fill="none" stroke={this._getLowerStrokeColor(1)} />
            <path d="M0 15 L5 10 L10 15" fill="none" stroke={this._getLowerStrokeColor(0)} />
          </g>
        </g>
      );
    }

    return (
      <div className={style.container}>
        <svg viewBox="0 0 300 100" className={style.number} width={width} height={height}>
          <defs>
            <path id="text-baseline" d="M0 45 H300" />
          </defs>
          <text textAnchor="middle" style={{ fontSize: this._calculateFontSize(), lineHeight: '100px' }}>
            <textPath xlinkHref="#text-baseline" ref={this._setAttribute('startOffset', '50%')}>
              <tspan ref={this._setAttribute('baseline-shift', '-40%')}>
                {this._formatData()}
              </tspan>
            </textPath>
          </text>
          {trendIndicators}
        </svg>
      </div>
    );
  },
});

export default NumericVisualization;
