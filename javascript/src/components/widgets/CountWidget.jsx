'use strict';

var React = require('react');
var numeral = require('numeral');

var BaseWidget = require('./BaseWidget');

var WidgetsStore = require('../../stores/widgets/WidgetsStore');

var CountWidget = React.createClass({
    DEFAULT_VALUE_FONT_SIZE: "70px",
    getInitialState() {
        return {
            result: undefined,
            calculatedAt: undefined
        };
    },
    loadValue() {
        var dataPromise = WidgetsStore.loadValue(this.props.dashboardId, this.props.widgetId);
        dataPromise.done((value) => {
            this.setState({
                result: value.result,
                calculatedAt: value.calculated_at
            });
        });
    },
    _calculateFontSize() {
        if (typeof this.state.result === 'undefined') {
            return this.DEFAULT_VALUE_FONT_SIZE;
        }

        var fontSize;
        var numberOfDigits = this._formatResultValue().replace(/[,.]/g, '').length;

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
    _formatResultValue() {
        try {
            return numeral(this.state.result).format("0,0.[00]");
        } catch(e) {}
    },
    render() {
        var widget = (
            <BaseWidget dashboardId={this.props.dashboardId}
                        widgetId={this.props.widgetId}
                        loadValueCallback={this.loadValue}
                        calculatedAt={this.state.calculatedAt}>
                <div className="count">
                    <div className="text-center">
                        <span className="value" style={{fontSize: this._calculateFontSize()}}>
                            {this._formatResultValue()}
                        </span>
                    </div>
                </div>
            </BaseWidget>
        );
        return widget;
    }
});

module.exports = CountWidget;