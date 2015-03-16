'use strict';

var React = require('react');
var numeral = require('numeral');

var NumberUtils = require("../../util/NumberUtils");

var TrendIndicatorType = {
    HIGHER: "higher",
    LOWER: "lower"
};

var CountVisualization = React.createClass({
    DEFAULT_VALUE_FONT_SIZE: "70px",
    NUMBER_OF_INDICATORS: 3,
    PERCENTAGE_PER_INDICATOR: 30,
    getInitialState() {
        return {
            nowCount: undefined,
            previousCount: undefined
        };
    },
    componentWillReceiveProps(newProps) {
        var result = newProps.data;
        if (typeof result === 'object') {
            var normalizedNowCount = NumberUtils.normalizeNumber(result.now);
            var normalizedPreviousCount = NumberUtils.normalizeNumber(result.previous);
            this.setState({
                nowCount: normalizedNowCount,
                previousCount: normalizedPreviousCount,
                percentage: this._calculatePercentage(normalizedNowCount, normalizedPreviousCount)
            }, this._calculatePercentage);
        } else {
            this.setState({nowCount: result});
        }
    },
    _calculatePercentage(nowCount, previousCount) {
        var percentage;
        if (previousCount === 0 || isNaN(previousCount)) {
            var factor = 0;
            if (nowCount > previousCount) {
                factor = 1;
            } else if (nowCount < previousCount) {
                factor = -1;
            }

            percentage = 100 * factor;
        } else {
            percentage = ((nowCount - previousCount) / previousCount) * 100;
        }

        return percentage;
    },
    _calculateFontSize() {
        if (typeof this.props.data === 'undefined') {
            return this.DEFAULT_VALUE_FONT_SIZE;
        }

        var fontSize;
        var numberOfDigits = this._formatData().replace(/[,.]/g, '').length;

        if (numberOfDigits < 7) {
            fontSize = this.DEFAULT_VALUE_FONT_SIZE;
        } else {
            switch (numberOfDigits) {
                case 7:
                    fontSize = "60px";
                    break;
                case 8:
                    fontSize = "50px";
                    break;
                case 9:
                case 10:
                    fontSize = "40px";
                    break;
                case 11:
                case 12:
                    fontSize = "35px";
                    break;
                default:
                    fontSize = "25px";
            }
        }

        return fontSize;
    },
    _formatData() {
        try {
            return numeral(this.state.nowCount).format("0,0.[00]");
        } catch(e) {
            return String(this.state.nowCount);
        }
    },
    _isIndicatorActive(index, trendIndicatorType) {
        if ((this.state.percentage === 0) ||
            (this.state.nowCount >= this.state.previousCount && trendIndicatorType !== TrendIndicatorType.HIGHER) ||
            (this.state.nowCount <= this.state.previousCount && trendIndicatorType !== TrendIndicatorType.LOWER)) {
            return false;
        }

        var reverseIndex = trendIndicatorType === TrendIndicatorType.HIGHER;

        if (reverseIndex) {
            index = Math.abs(index - (this.NUMBER_OF_INDICATORS - 1));
        }
        return Math.abs(this.state.percentage) >= this.PERCENTAGE_PER_INDICATOR * index;
    },
    _getIndicatorClass(index, trendIndicatorType) {
        var className = "trend-icon";

        var indicatorIsActive = this._isIndicatorActive(index, trendIndicatorType);
        if(!indicatorIsActive) {
            return className;
        }

        var lowerClass = Boolean(this.props.config.lower_is_better) ? "trend-good" : "trend-bad";
        var higherClass = Boolean(this.props.config.lower_is_better) ? "trend-bad" : "trend-good";

        var activeClass = trendIndicatorType === TrendIndicatorType.HIGHER ? higherClass : lowerClass;

        return className + " " + activeClass;
    },
    _getHigherIndicatorClass(index) {
        return this._getIndicatorClass(index, TrendIndicatorType.HIGHER);
    },
    _getLowerIndicatorClass(index) {
        return this._getIndicatorClass(index, TrendIndicatorType.LOWER);
    },
    render() {
        var trendIndicators;

        if (Boolean(this.props.config.trend)) {
            trendIndicators = (
                <div className="trend-indicators">
                    <div className="trend-icons-higher">
                        <div className={this._getHigherIndicatorClass(0)}>
                            <span className="trend-higher"><i className="fa fa-angle-up"></i></span>
                        </div>
                        <div className={this._getHigherIndicatorClass(1)}>
                            <span className="trend-higher"><i className="fa fa-angle-up"></i></span>
                        </div>
                        <div className={this._getHigherIndicatorClass(2)}>
                            <span className="trend-higher"><i className="fa fa-angle-up"></i></span>
                        </div>
                    </div>
                    <div className="trend-icons-lower">
                        <div className={this._getLowerIndicatorClass(0)}>
                            <span className="trend-lower"><i className="fa fa-angle-down"></i></span>
                        </div>
                        <div className={this._getLowerIndicatorClass(1)}>
                            <span className="trend-lower"><i className="fa fa-angle-down"></i></span>
                        </div>
                        <div className={this._getLowerIndicatorClass(2)}>
                            <span className="trend-lower"><i className="fa fa-angle-down"></i></span>
                        </div>
                    </div>
                </div>
            );
        }

        return (
            <div className="count">
                <div className="text-center">
                    <span className="value" style={{fontSize: this._calculateFontSize()}}>
                        {this._formatData()}
                    </span>
                    {trendIndicators}
                </div>
            </div>
        );
    }
});

module.exports = CountVisualization;