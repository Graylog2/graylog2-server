"use strict";

var React = require('react');

var WidgetHeader = React.createClass({
    render() {
        return (
            <div>
                <div className="widget-title">
                    {this.props.title}
                </div>
                <div className="widget-calculated-at" title={this.props.calculatedAt}>
                    {moment(this.props.calculatedAt).fromNow()}
                </div>
            </div>
        );
    }
});

module.exports = WidgetHeader;