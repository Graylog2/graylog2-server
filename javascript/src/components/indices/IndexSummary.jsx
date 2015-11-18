import React from 'react';
import Reflux from 'reflux';
import numeral from 'numeral';
import moment from 'moment';
import { Collapse } from 'react-bootstrap';

import { IndexSizeSummary } from 'components/indices';

const IndexSummary = React.createClass({
  propTypes: {
    children: React.PropTypes.node.isRequired,
    index: React.PropTypes.object.isRequired,
    indexRange: React.PropTypes.object.isRequired,
    isDeflector: React.PropTypes.bool.isRequired,
  },
  getInitialState() {
    return { showDetails: this.props.isDeflector };
  },
  _deflectorBadge() {
    if (this.props.isDeflector) {
      return <i className="fa fa-bolt" title="Write-active index"/>;
    }
  },
  _reopenedBadged(index) {
    if (index.reopened) {
      return <i className="fa fa-retweet" title="Reopened index"/>;
    }
  },
  _formatIndexRange() {
    if (this.props.isDeflector) {
      return 'Contains messages up to ' + moment().fromNow();
    }

    if (this.props.index.all_shards) {
      const count = this.props.index.all_shards.documents.count;
      const deleted = this.props.index.all_shards.documents.deleted;
      if (count === 0 || count - deleted === 0) {
        return 'Index does not contain any messages.';
      }
    }

    if (!this.props.indexRange) {
      return 'Time range of index is unknown, because index range is not available. Please recalculate index ranges manually.';
    }

    if (this.props.indexRange.begin === 0) {
      return 'Contains messages up to ' + moment(this.props.indexRange.end).fromNow();
    }

    return 'Contains messages from ' + moment(this.props.indexRange.begin).fromNow() + ' up to ' + moment(this.props.indexRange.end).fromNow();
  },
  _formatShowDetailsLink() {
    if (this.state.showDetails) {
      return <span><i className="fa fa-caret-down"/> Hide Details / Actions</span>;
    }
    return <span><i className="fa fa-caret-right"/> Show Details / Actions</span>;
  },
  _toggleShowDetails() {
    this.setState({ showDetails: !this.state.showDetails });
  },
  render() {
    const index = this.props.index;
    return (
      <span>
        <h2>
          {this._deflectorBadge()}{' '}

          {index.name}{' '}

          {this._reopenedBadged(index)}{' '}

          <small>
            {this._formatIndexRange(index)}{' '}

            <IndexSizeSummary index={index} />

            <a onClick={this._toggleShowDetails}>{this._formatShowDetailsLink()}</a>
          </small>
        </h2>

        <div className="index-info-holder">
          <Collapse in={this.state.showDetails} timeout={0}>
            {this.props.children}
          </Collapse>
        </div>
      </span>
    );
  },
});

export default IndexSummary;
