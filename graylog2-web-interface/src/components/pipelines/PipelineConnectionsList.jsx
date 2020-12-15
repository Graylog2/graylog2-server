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
import naturalSort from 'javascript-natural-sort';

class PipelineConnectionsList extends React.Component {
  static propTypes = {
    pipeline: PropTypes.object.isRequired,
    connections: PropTypes.array.isRequired,
    streams: PropTypes.array.isRequired,
    streamsFormatter: PropTypes.func.isRequired,
    noConnectionsMessage: PropTypes.any,
  };

  static defaultProps = {
    noConnectionsMessage: 'Pipeline not connected to any streams',
  };

  render() {
    const streamsUsingPipeline = this.props.connections
      .filter((c) => c.pipeline_ids && c.pipeline_ids.includes(this.props.pipeline.id)) // Get connections for this pipeline
      .filter((c) => this.props.streams.some((s) => s.id === c.stream_id)) // Filter out deleted streams
      .map((c) => this.props.streams.find((s) => s.id === c.stream_id))
      .sort((s1, s2) => naturalSort(s1.title, s2.title));

    return (
      <span>
        {streamsUsingPipeline.length === 0 ? this.props.noConnectionsMessage : this.props.streamsFormatter(streamsUsingPipeline)}
      </span>
    );
  }
}

export default PipelineConnectionsList;
