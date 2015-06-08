"use strict";

var React = require('react');

var WidgetHeader = React.createClass({
    render() {
        var loadErrorElement = null;

        if (this.props.error) {
            loadErrorElement = (
                <span className="load-error" title={this.props.errorMessage}><i className="fa fa-exclamation-triangle"></i></span>
            );
        }

        return (
            <div>
                <div className="widget-update-info">
                    {loadErrorElement}
                    <span title={this.props.calculatedAt}>{moment(this.props.calculatedAt).fromNow()}</span>
                </div>
                <div className="widget-title">
                    {this.props.title}
                </div>
                <div className="clearfix"></div>
            </div>
        );
    }
});

module.exports = WidgetHeader;