import React from 'react';
import naturalSort from 'javascript-natural-sort';

const PipelineConnectionsList = React.createClass({
  propTypes: {
    pipeline: React.PropTypes.object.isRequired,
    connections: React.PropTypes.array.isRequired,
    streams: React.PropTypes.array.isRequired,
    streamsFormatter: React.PropTypes.func.isRequired,
    noConnectionsMessage: React.PropTypes.any,
  },

  getDefaultProps() {
    return {
      noConnectionsMessage: 'Pipeline not connected to any streams',
    };
  },

  render() {
    const streamsUsingPipeline = this.props.connections
      .filter(c => c.pipeline_ids && c.pipeline_ids.includes(this.props.pipeline.id)) // Get connections for this pipeline
      .filter(c => this.props.streams.some(s => s.id === c.stream_id)) // Filter out deleted streams
      .map(c => this.props.streams.find(s => s.id === c.stream_id))
      .sort((s1, s2) => naturalSort(s1.title, s2.title));

    return (
      <span>
        {streamsUsingPipeline.length === 0 ? this.props.noConnectionsMessage : this.props.streamsFormatter(streamsUsingPipeline)}
      </span>
    );
  },
});

export default PipelineConnectionsList;
