"use strict";

var React = require('react');

var WidgetFooter = React.createClass({
    _replaySearch(e) {
        this.props.onReplaySearch(e);
    },
    _showConfig() {
        this.props.onShowConfig();
    },
    _editConfig() {
        this.props.onEditConfig();
    },
    _delete() {
        this.props.onDelete();
    },
    render() {
        var lockedActions = (
            <div className="actions">
                <div className="widget-replay">
                    <button className="btn btn-mini btn-link btn-text"
                            title="Replay search"
                            onClick={this._replaySearch}>
                        <i className="fa fa-play"></i>
                    </button>
                </div>
                <div className="widget-info">
                    <button className="btn btn-mini btn-link btn-text"
                            title="Show widget configuration"
                            onClick={this._showConfig}>
                        <i className="fa fa-info-circle"></i>
                    </button>
                </div>
            </div>
        );

        var unlockedActions = (
            <div className="actions">
                <div className="widget-delete">
                    <button className="btn btn-mini btn-link btn-text"
                            title="Delete widget"
                            onClick={this._delete}>
                        <i className="fa fa-trash"></i>
                    </button>
                </div>
                <div className="widget-edit">
                    <button className="btn btn-mini btn-link btn-text"
                            title="Edit widget"
                            onClick={this._editConfig}>
                        <i className="fa fa-pencil"></i>
                    </button>
                </div>
            </div>
        );

        return (
            <div>
                {this.props.locked ? lockedActions : unlockedActions}
            </div>
        );
    }
});

module.exports = WidgetFooter;