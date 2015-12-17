import React from 'react';

const WidgetFooter = React.createClass({
  propTypes: {
    locked: React.PropTypes.bool.isRequired,
    onDelete: React.PropTypes.func.isRequired,
    onEditConfig: React.PropTypes.func.isRequired,
    onReplaySearch: React.PropTypes.func.isRequired,
    onShowConfig: React.PropTypes.func.isRequired,
  },
  _replaySearch(e) {
    e.preventDefault();
    this.props.onReplaySearch(e);
  },
  _showConfig(e) {
    e.preventDefault();
    this.props.onShowConfig();
  },
  _editConfig(e) {
    e.preventDefault();
    this.props.onEditConfig();
  },
  _delete(e) {
    e.preventDefault();
    this.props.onDelete();
  },
  render() {
    const lockedActions = (
      <div className="actions">
        <div className="widget-replay">
          <button className="btn btn-mini btn-link btn-text"
                  title="Replay search"
                  onClick={this._replaySearch}>
            <i className="fa fa-play"/>
          </button>
        </div>
        <div className="widget-info">
          <button className="btn btn-mini btn-link btn-text"
                  title="Show widget configuration"
                  onClick={this._showConfig}>
            <i className="fa fa-info-circle"/>
          </button>
        </div>
      </div>
    );

    const unlockedActions = (
      <div className="actions">
        <div className="widget-delete">
          <button className="btn btn-mini btn-link btn-text"
                  title="Delete widget"
                  onClick={this._delete}>
            <i className="fa fa-trash"/>
          </button>
        </div>
        <div className="widget-edit">
          <button className="btn btn-mini btn-link btn-text"
                  title="Edit widget"
                  onClick={this._editConfig}>
            <i className="fa fa-pencil"/>
          </button>
        </div>
      </div>
    );

    return (
      <div>
        {this.props.locked ? lockedActions : unlockedActions}
      </div>
    );
  },
});

export default WidgetFooter;
