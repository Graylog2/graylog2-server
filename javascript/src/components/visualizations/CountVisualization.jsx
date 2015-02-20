'use strict';

var React = require('react');
var numeral = require('numeral');

var CountVisualization = React.createClass({
    DEFAULT_VALUE_FONT_SIZE: "70px",
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
            return numeral(this.props.data).format("0,0.[00]");
        } catch(e) {}
    },
    render() {
        return (
            <div className="count">
                <div className="text-center">
                    <span className="value" style={{fontSize: this._calculateFontSize()}}>
                        {this._formatData()}
                    </span>
                </div>
            </div>
        );
    }
});

module.exports = CountVisualization;