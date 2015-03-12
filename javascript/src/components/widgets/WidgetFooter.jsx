"use strict";

var React = require('react');

var WidgetFooter = React.createClass({
    _replaySearch() {
        this.props.onReplaySearch();
    },
    _showConfig() {
        this.props.onShowConfig();
    },
    render() {
        return (
            <div>
                <div className="actions">
                    <div className="widget-replay">
                        <button className="btn btn-mini btn-link btn-text"
                                title="Replay search"
                                onClick={this._replaySearch}>
                            <i className="icon icon-play"></i>
                        </button>
                    </div>
                    <div className="widget-info">
                        <button className="btn btn-mini btn-link btn-text"
                                title="Show widget configuration"
                                onClick={this._showConfig}>
                            <i className="icon icon-cog"></i>
                        </button>
                    </div>
                </div>
            </div>
        );
    }
});

module.exports = WidgetFooter;