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
import createReactClass from 'create-react-class';
import styled from 'styled-components';

import { Alert } from 'components/graylog';
import { Icon } from 'components/common';
import PermissionsMixin from 'util/PermissionsMixin';

import Stream from './Stream';

const StreamsList = styled.ul`
  padding: 0;
  margin: 0;
`;

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
        <StreamsList>
          {streamList}
        </StreamsList>
      );
    }

    return (
      <Alert bsStyle="info">
        <Icon name="info-circle" />&nbsp;No streams match your search filter.
      </Alert>
    );
  },
});

export default StreamList;
