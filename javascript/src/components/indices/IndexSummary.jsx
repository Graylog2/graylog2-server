import React from 'react';
import Reflux from 'reflux';
import numeral from 'numeral';
import moment from 'moment';
import { Collapse } from 'react-bootstrap';

import DeflectorStore from 'stores/indices/DeflectorStore';

const IndexSummary = React.createClass({
  mixins: [Reflux.connect(DeflectorStore)],
  propTypes: {
    children: React.PropTypes.node.isRequired,
    index: React.PropTypes.object.isRequired,
    indexRange: React.PropTypes.object.isRequired,
  },
  getInitialState() {
    return { showDetails: this._isDeflector(this.props.index) };
  },
  _isDeflector(index) {
    return index.name === this.props.deflector.info.current_target;
  },
  _deflectorBadge(index) {
    if (this._isDeflector(index)) {
      return <i className="fa fa-bolt" title="Write-active index"/>;
    }
  },
  _reopenedBadged(index) {
    if (index.reopened) {
      return <i className="fa fa-retweet" title="Reopened index"/>;
    }
  },
  _formatIndexRange(index) {
    if (this._isDeflector(index)) {
      return 'up to ' + moment().fromNow();
    }

    if (this.props.indexRange.begin === 0) {
      return 'up to ' + moment(this.props.indexRange.end).fromNow();
    }

    return 'from ' + moment(this.props.indexRange.begin).fromNow() + ' up to ' + moment(this.props.indexRange.end).fromNow();
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
          {this._deflectorBadge(index)}{' '}

          {index.name}{' '}

          {this._reopenedBadged(index)}{' '}

          <small>
            Contains messages {this._formatIndexRange(index)}{' '}

            ({numeral(index.all_shards.store_size_bytes).format('0.0b')} / {numeral(index.all_shards.documents.count).format('0,0')} messages){' '}

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
