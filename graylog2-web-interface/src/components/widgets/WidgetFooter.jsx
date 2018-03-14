import PropTypes from 'prop-types';
import React from 'react';
import { Button } from 'react-bootstrap';
import { Timestamp } from 'components/common';

class WidgetFooter extends React.Component {
  static propTypes = {
    locked: PropTypes.bool.isRequired,
    onDelete: PropTypes.func.isRequired,
    onEditConfig: PropTypes.func.isRequired,
    onShowConfig: PropTypes.func.isRequired,
    replayHref: PropTypes.string.isRequired,
    error: PropTypes.any,
    errorMessage: PropTypes.string,
    calculatedAt: PropTypes.string,
    replayDisabled: PropTypes.bool,
  };

  _showConfig = (e) => {
    e.preventDefault();
    this.props.onShowConfig();
  };

  _editConfig = (e) => {
    e.preventDefault();
    this.props.onEditConfig();
  };

  _delete = (e) => {
    e.preventDefault();
    this.props.onDelete();
  };

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

    const replay = this.props.replayDisabled ? null : (
      <div className="widget-replay">
        <Button bsStyle="link" className="btn-text" title="Replay search" href={this.props.replayHref}>
          <i className="fa fa-play" />
        </Button>
      </div>
    );
    const lockedActions = (
      <div className="actions">
        {replay}
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
  }
}

export default WidgetFooter;
