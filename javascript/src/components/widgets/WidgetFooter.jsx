"use strict";

var React = require('react');

var WidgetFooter = React.createClass({
    render() {
        return (
            <div>
                <div className="actions">
                    <div className="widget-replay">
                        <a href={this.props.replayUrl}>
                            <i className="icon icon-play-sign"></i>
                        </a>
                    </div>
                    <div className="widget-info">
                        <a href="#">
                            <i className="icon icon-info-sign"></i>
                        </a>
                    </div>
                </div>
            </div>
        );
    }
});

module.exports = WidgetFooter;