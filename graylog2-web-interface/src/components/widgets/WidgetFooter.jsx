import PropTypes from 'prop-types';
import React from 'react';
import { Button, OverlayTrigger, Tooltip } from 'react-bootstrap';
import { Timestamp } from 'components/common';

const WidgetFooter = React.createClass({
  propTypes: {
    locked: PropTypes.bool.isRequired,
    onDelete: PropTypes.func.isRequired,
    onEditConfig: PropTypes.func.isRequired,
    onShowConfig: PropTypes.func.isRequired,
    replayHref: PropTypes.string.isRequired,
    error: PropTypes.any,
    errorMessage: PropTypes.string,
    calculatedAt: PropTypes.string,
    replayToolTip: PropTypes.string,
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
    let loadErrorElement;

    if (this.props.error) {
      loadErrorElement = (
        <span className="load-error" title={this.props.errorMessage}>
          <i className="fa fa-exclamation-triangle" />
        </span>
      );
    }

    let calculatedAtTime;

    if (this.props.calculatedAt) {
      calculatedAtTime = <span title={this.props.calculatedAt}><Timestamp dateTime={this.props.calculatedAt} relative /></span>;
    } else {
      calculatedAtTime = 'Loading...';
    }

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
        <div className="widget-update-info">
        {loadErrorElement}
        {calculatedAtTime}
        </div>
        <div>
          {this.props.locked ? lockedActions : unlockedActions}
        </div>
      </div>
    );
  },
});

export default WidgetFooter;
