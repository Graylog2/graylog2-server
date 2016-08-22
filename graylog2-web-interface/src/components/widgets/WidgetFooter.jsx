import React from 'react';
import { Button } from 'react-bootstrap';

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
          <Button bsStyle="link" className="btn-text" title="Replay search" onClick={this._replaySearch}>
            <i className="fa fa-play"/>
          </Button>
        </div>
        <div className="widget-info">
          <Button bsStyle="link" className="btn-text" title="Show widget configuration" onClick={this._showConfig}>
            <i className="fa fa-info-circle"/>
          </Button>
        </div>
      </div>
    );

    const unlockedActions = (
      <div className="actions">
        <div className="widget-delete">
          <Button bsStyle="link" className="btn-text" title="Delete widget" onClick={this._delete}>
            <i className="fa fa-trash"/>
          </Button>
        </div>
        <div className="widget-edit">
          <Button bsStyle="link" className="btn-text" title="Edit widget" onClick={this._editConfig}>
            <i className="fa fa-pencil"/>
          </Button>
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
