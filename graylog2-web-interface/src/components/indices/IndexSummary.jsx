/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import PropTypes from 'prop-types';
import React from 'react';

import { Label } from 'components/graylog';
import { Timestamp, Icon } from 'components/common';
import DateTime from 'logic/datetimes/DateTime';
import { IndexSizeSummary } from 'components/indices';

class IndexSummary extends React.Component {
  static propTypes = {
    children: PropTypes.node.isRequired,
    index: PropTypes.object.isRequired,
    indexRange: PropTypes.object,
    isDeflector: PropTypes.bool.isRequired,
    name: PropTypes.string.isRequired,
  };

  state = { showDetails: this.props.isDeflector };

  _formatLabels = (index) => {
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
  };

  _formatIndexRange = () => {
    if (this.props.isDeflector) {
      return <span>Contains messages up to <Timestamp dateTime={new DateTime().toISOString()} relative /></span>;
    }

    const sizes = this.props.index.size;

    if (sizes) {
      const count = sizes.events;
      const { deleted } = sizes;

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
  };

  _formatShowDetailsLink = () => {
    if (this.state.showDetails) {
      return <span className="index-more-actions"><Icon name="caret-down" /> Hide Details / Actions</span>;
    }

    return <span className="index-more-actions"><Icon name="caret-right" /> Show Details / Actions</span>;
  };

  _toggleShowDetails = (event) => {
    event.preventDefault();
    this.setState({ showDetails: !this.state.showDetails });
  };

  render() {
    const { index } = this.props;

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
  }
}

export default IndexSummary;
