import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import { Alert } from 'react-bootstrap';

import PermissionsMixin from 'util/PermissionsMixin';
import Stream from './Stream';

const StreamList = createReactClass({
  displayName: 'StreamList',

  propTypes: {
    streams: PropTypes.array.isRequired,
    streamRuleTypes: PropTypes.array.isRequired,
    indexSets: PropTypes.array.isRequired,
    user: PropTypes.object.isRequired,
    permissions: PropTypes.array.isRequired,
  },

  mixins: [PermissionsMixin],

  getInitialState() {
    return {};
  },

  _formatStream(stream) {
    return (
      <Stream key={`stream-${stream.id}`}
              stream={stream}
              streamRuleTypes={this.props.streamRuleTypes}
              permissions={this.props.permissions}
              user={this.props.user}
              indexSets={this.props.indexSets} />
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
