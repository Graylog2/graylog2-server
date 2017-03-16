import React from 'react';
import { Alert } from 'react-bootstrap';

import Stream from './Stream';
import PermissionsMixin from 'util/PermissionsMixin';

const StreamList = React.createClass({
  propTypes: {
    streams: React.PropTypes.array.isRequired,
    streamRuleTypes: React.PropTypes.array.isRequired,
    indexSets: React.PropTypes.array.isRequired,
    user: React.PropTypes.object.isRequired,
    permissions: React.PropTypes.array.isRequired,
    onStreamSave: React.PropTypes.func.isRequired,
  },
  mixins: [PermissionsMixin],

  getInitialState() {
    return {};
  },

  _formatStream(stream) {
    return (
      <Stream key={`stream-${stream.id}`} stream={stream} streamRuleTypes={this.props.streamRuleTypes}
                   permissions={this.props.permissions} user={this.props.user} indexSets={this.props.indexSets} />
    );
  },

  _sortByTitle(stream1, stream2) {
    return stream1.title.localeCompare(stream2.title);
  },

  render() {
    if (this.props.streams.length > 0) {
      const streamList = this.props.streams.sort(this._sortByTitle).map(this._formatStream);

      return (
        <ul className="streams">
          {streamList}
        </ul>
      );
    }
    return (
      <Alert bsStyle="info">
        <i className="fa fa-info-circle" />&nbsp;No streams match your search filter.
        </Alert>
    );
  },
});

export default StreamList;
