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

import StreamRule from 'components/streamrules/StreamRule';
import { Spinner } from 'components/common';
import { ListGroup, ListGroupItem } from 'components/graylog';

class StreamRuleList extends React.Component {
  static propTypes = {
    matchData: PropTypes.shape({
      matches: PropTypes.bool,
      rules: PropTypes.object,
    }),
    onSubmit: PropTypes.func,
    onDelete: PropTypes.func,
    permissions: PropTypes.array.isRequired,
    stream: PropTypes.object.isRequired,
    streamRuleTypes: PropTypes.array.isRequired,
  };

  static defaultProps = {
    matchData: {},
    onSubmit: () => {},
    onDelete: () => {},
  }

  _formatStreamRules = (streamRules) => {
    if (streamRules && streamRules.length > 0) {
      const {
        matchData,
        onDelete,
        onSubmit,
        permissions,
        stream,
        streamRuleTypes,
      } = this.props;

      return streamRules.map((streamRule) => (
        <StreamRule key={streamRule.id}
                    permissions={permissions}
                    matchData={matchData}
                    onSubmit={onSubmit}
                    onDelete={onDelete}
                    stream={stream}
                    streamRule={streamRule}
                    streamRuleTypes={streamRuleTypes} />
      ));
    }

    return <ListGroupItem>No rules defined.</ListGroupItem>;
  };

  render() {
    const { stream } = this.props;

    if (stream) {
      const streamRules = this._formatStreamRules(stream.rules);

      return (
        <ListGroup componentClass="ul">
          {streamRules}
        </ListGroup>
      );
    }

    return <Spinner />;
  }
}

export default StreamRuleList;
