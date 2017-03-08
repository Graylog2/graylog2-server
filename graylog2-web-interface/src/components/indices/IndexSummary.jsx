import React from 'react';
import { Label } from 'react-bootstrap';

import { Timestamp } from 'components/common';
import DateTime from 'logic/datetimes/DateTime';

import { IndexSizeSummary } from 'components/indices';

const IndexSummary = React.createClass({
  propTypes: {
    children: React.PropTypes.node.isRequired,
    index: React.PropTypes.object.isRequired,
    indexRange: React.PropTypes.object,
    isDeflector: React.PropTypes.bool.isRequired,
    name: React.PropTypes.string.isRequired,
  },
  getInitialState() {
    return { showDetails: this.props.isDeflector };
  },
  _formatLabels(index) {
    const labels = [];
    if (index.is_deflector) {
      labels.push(<Label key={`${this.props.name}-deflector-label`} bsStyle="primary">active write index</Label>);
    }

    if (index.is_closed) {
      labels.push(<Label key={`${this.props.name}-closed-label`} bsStyle="warning">closed</Label>);
    }

    if (index.is_reopened) {
      labels.push(<Label key={`${this.props.name}-reopened-label`} bsStyle="success">reopened</Label>);
    }

    return <span className="index-label">{labels}</span>;
  },

  _formatIndexRange() {
    if (this.props.isDeflector) {
      return <span>Contains messages up to <Timestamp dateTime={new DateTime().toISOString()} relative /></span>;
    }

    const sizes = this.props.index.size;
    if (sizes) {
      const count = sizes.events;
      const deleted = sizes.deleted;
      if (count === 0 || count - deleted === 0) {
        return 'Index does not contain any messages.';
      }
    }

    if (!this.props.indexRange) {
      return 'Time range of index is unknown, because index range is not available. Please recalculate index ranges manually.';
    }

    if (this.props.indexRange.begin === 0) {
      return <span>Contains messages up to <Timestamp dateTime={this.props.indexRange.end} relative /></span>;
    }

    return (
      <span>
        Contains messages from <Timestamp dateTime={this.props.indexRange.begin} relative /> up to{' '}
        <Timestamp dateTime={this.props.indexRange.end} relative />
      </span>
    );
  },
  _formatShowDetailsLink() {
    if (this.state.showDetails) {
      return <span className="index-more-actions"><i className="fa fa-caret-down" /> Hide Details / Actions</span>;
    }
    return <span className="index-more-actions"><i className="fa fa-caret-right" /> Show Details / Actions</span>;
  },
  _toggleShowDetails(event) {
    event.preventDefault();
    this.setState({ showDetails: !this.state.showDetails });
  },
  render() {
    const index = this.props.index;
    return (
      <span>
        <h2>
          {this.props.name}{' '}

          <small>
            {this._formatLabels(index)}{' '}
            {this._formatIndexRange(index)}{' '}

            <IndexSizeSummary index={index} />

            <a onClick={this._toggleShowDetails} href="#">{this._formatShowDetailsLink()}</a>
          </small>
        </h2>

        <div className="index-info-holder">
          {this.state.showDetails && this.props.children}
        </div>
      </span>
    );
  },
});

export default IndexSummary;
