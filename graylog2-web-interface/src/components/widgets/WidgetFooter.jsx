import React from 'react';
import { Button, OverlayTrigger, Tooltip } from 'react-bootstrap';

const WidgetFooter = React.createClass({
  propTypes: {
    locked: React.PropTypes.bool.isRequired,
    onDelete: React.PropTypes.func.isRequired,
    onEditConfig: React.PropTypes.func.isRequired,
    onShowConfig: React.PropTypes.func.isRequired,
    replayHref: React.PropTypes.string.isRequired,
    replayToolTip: React.PropTypes.string,
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
    // if we have a tooltip, we disable the button link and instead show a tooltip on hover
    const title = this.props.replayToolTip ? null : 'Replay search';
    const href = this.props.replayToolTip ? null : this.props.replayHref;
    let replay = (
      <Button bsStyle="link" className="btn-text" title={title} href={href}>
        <i className="fa fa-play" />
      </Button>
    );
    if (this.props.replayToolTip) {
      replay = (
        <OverlayTrigger placement="bottom" overlay={<Tooltip id="tooltip">{this.props.replayToolTip}</Tooltip>}>
          {replay}
        </OverlayTrigger>
      );
    }
    const lockedActions = (
      <div className="actions">
        <div className="widget-replay">
          {replay}
        </div>
        <div className="widget-info">
          <Button bsStyle="link" className="btn-text" title="Show widget configuration" onClick={this._showConfig}>
            <i className="fa fa-info-circle" />
          </Button>
        </div>
      </div>
    );

    const unlockedActions = (
      <div className="actions">
        <div className="widget-delete">
          <Button bsStyle="link" className="btn-text" title="Delete widget" onClick={this._delete}>
            <i className="fa fa-trash" />
          </Button>
        </div>
        <div className="widget-edit">
          <Button bsStyle="link" className="btn-text" title="Edit widget" onClick={this._editConfig}>
            <i className="fa fa-pencil" />
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
