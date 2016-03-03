import React from 'react';
import { Row, Col } from 'react-bootstrap';

import { EntityList, TypeAheadDataFilter } from 'components/common';
import Connection from './Connection';
import ConnectionForm from './ConnectionForm';

const PipelineConnections = React.createClass({
  propTypes: {
    pipelines: React.PropTypes.array.isRequired,
    streams: React.PropTypes.array.isRequired,
    connections: React.PropTypes.array.isRequired,
    onConnectionsChange: React.PropTypes.func.isRequired,
  },

  _updateConnection(newConnection, callback) {
    this.props.onConnectionsChange(newConnection, callback);
  },

  render() {
    // TODO: Sort this list by stream title
    const formattedConnections = this.props.connections.map(c => {
      return (
        <Connection key={c.stream_id} stream={this.props.streams.filter(s => s.id === c.stream_id)[0]}
                    pipelines={this.props.pipelines.filter(p => c.pipeline_ids.indexOf(p.id) !== -1)}
                    onUpdate={this._updateConnection}/>
      );
    });

    const filteredStreams = this.props.streams.filter(s => !this.props.connections.some(c => c.stream_id === s.id));

    return (
      <div>
        <Row className="row-sm">
          <Col md={8}>
            <TypeAheadDataFilter label="Filter streams"
                                 data={this.props.streams}
                                 displayKey={'title'}
                                 filterSuggestions={[]}
                                 searchInKeys={['title', 'description']}
                                 onDataFiltered={() => {}}/>
          </Col>
          <Col md={4}>
            <div className="pull-right">
              <ConnectionForm create streams={filteredStreams} save={this._updateConnection}/>
            </div>
          </Col>
        </Row>
        <EntityList bsNoItemsStyle="info" noItemsText="There are no pipeline connections."
                    items={formattedConnections}/>
      </div>
    );
  },
});

export default PipelineConnections;
