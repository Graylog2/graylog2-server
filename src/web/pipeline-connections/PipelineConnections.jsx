import React from 'react';
import { Row, Col } from 'react-bootstrap';
import naturalSort from 'javascript-natural-sort';
import { LinkContainer } from 'react-router-bootstrap';

import { EntityList, TypeAheadDataFilter } from 'components/common';
import Connection from './Connection';
import ConnectionForm from './ConnectionForm';
import Routes from 'routing/Routes';

const PipelineConnections = React.createClass({
  propTypes: {
    pipelines: React.PropTypes.array.isRequired,
    streams: React.PropTypes.array.isRequired,
    connections: React.PropTypes.array.isRequired,
    onConnectionsChange: React.PropTypes.func.isRequired,
  },

  getInitialState() {
    return {
      filteredStreams: this.props.streams.slice(),
    };
  },

  _updateFilteredStreams(filteredStreams) {
    this.setState({ filteredStreams });
  },

  _updateConnection(newConnection, callback) {
    this.props.onConnectionsChange(newConnection, callback);
  },

  _isConnectionWithPipelines(connection) {
    return connection.pipeline_ids.length > 0;
  },

  render() {
    const filteredStreamsMap = {};
    this.state.filteredStreams.forEach(s => {
      filteredStreamsMap[s.id] = s;
    });

    const formattedConnections = this.props.connections
      .filter(c => this.state.filteredStreams.some(s => s.id === c.stream_id && this._isConnectionWithPipelines(c)))
      .sort((c1, c2) => naturalSort(filteredStreamsMap[c1.stream_id].title, filteredStreamsMap[c2.stream_id].title))
      .map(c => {
        return (
          <Connection key={c.stream_id} stream={this.props.streams.filter(s => s.id === c.stream_id)[0]}
                      pipelines={this.props.pipelines.filter(p => c.pipeline_ids.indexOf(p.id) !== -1)}
                      onUpdate={this._updateConnection} />
        );
      });

    const filteredStreams = this.props.streams.filter(s => !this.props.connections.some(c => c.stream_id === s.id && this._isConnectionWithPipelines(c)));
    const pipelinesNo = this.props.pipelines.length;
    let noItemsText;
    if (pipelinesNo === 0) {
      noItemsText = (
        <span>
          There are no pipeline connections. You can start by creating your{' '}
          <LinkContainer to={Routes.pluginRoute('SYSTEM_PIPELINES_RULES')}>
            <a>pipeline rules</a>
          </LinkContainer>{' '}
          and then putting them together in a{' '}
          <LinkContainer to={Routes.pluginRoute('SYSTEM_PIPELINES_OVERVIEW')}>
            <a>pipeline</a>
          </LinkContainer>!
        </span>
      );
    } else {
      noItemsText = (
        <span>
          There are no pipeline connections. Click on "Add new connection" to connect your pipelines to a stream.
        </span>
      );
    }

    return (
      <div>
        <Row className="row-sm">
          <Col md={8}>
            <TypeAheadDataFilter label="Filter streams"
                                 data={this.props.streams}
                                 displayKey={'title'}
                                 filterSuggestions={[]}
                                 searchInKeys={['title', 'description']}
                                 onDataFiltered={this._updateFilteredStreams} />
          </Col>
          <Col md={4}>
            <div className="pull-right">
              <ConnectionForm create streams={filteredStreams} save={this._updateConnection} />
            </div>
          </Col>
        </Row>
        <EntityList bsNoItemsStyle="info" noItemsText={noItemsText}
                    items={formattedConnections} />
      </div>
    );
  },
});

export default PipelineConnections;
