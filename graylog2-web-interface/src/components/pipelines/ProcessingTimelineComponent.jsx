import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import styled from 'styled-components';
import { LinkContainer } from 'react-router-bootstrap';
import { Link } from 'react-router';
import naturalSort from 'javascript-natural-sort';

import { Alert, Button } from 'components/graylog';
import { DataTable, Spinner } from 'components/common';
import { MetricContainer, CounterRate } from 'components/metrics';

import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';

import PipelineConnectionsList from './PipelineConnectionsList';

const { PipelinesStore, PipelinesActions } = CombinedProvider.get('Pipelines');
const { PipelineConnectionsStore, PipelineConnectionsActions } = CombinedProvider.get('PipelineConnections');
const { StreamsStore } = CombinedProvider.get('Streams');

const StyledAlert = styled(Alert)`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px;
`;

const ProcessingTimelineComponent = createReactClass({
  displayName: 'ProcessingTimelineComponent',
  mixins: [Reflux.connect(PipelinesStore), Reflux.connect(PipelineConnectionsStore)],

  componentDidMount() {
    this.style.use();
    PipelinesActions.list();
    PipelineConnectionsActions.list();

    StreamsStore.listStreams().then((streams) => {
      this.setState({ streams });
    });
  },

  componentWillUnmount() {
    this.style.unuse();
  },

  // eslint-disable-next-line
  style: require('!style/useable!css!./ProcessingTimelineComponent.css'),

  _calculateUsedStages(pipelines) {
    return pipelines
      .map(pipeline => pipeline.stages)
      .reduce((usedStages, pipelineStages) => {
        // Concat stages in a single array removing duplicates
        return usedStages.concat(pipelineStages.map(stage => stage.stage).filter(stage => usedStages.indexOf(stage) === -1));
      }, [])
      .sort(naturalSort);
  },

  _headerCellFormatter(header) {
    let className;
    if (header === 'Actions') {
      className = 'actions';
    }

    return <th className={className}>{header}</th>;
  },

  _formatConnectedStreams(streams) {
    return streams.map(s => s.title).join(', ');
  },

  _formatStages(pipeline, stages) {
    const formattedStages = [];
    const stageNumbers = stages.map(stage => stage.stage);

    this.usedStages.forEach((usedStage) => {
      if (stageNumbers.indexOf(usedStage) === -1) {
        formattedStages.push(
          <div key={`${pipeline.id}-stage${usedStage}`} className="pipeline-stage idle-stage">Idle</div>,
        );
      } else {
        formattedStages.push(
          <div key={`${pipeline.id}-stage${usedStage}`} className="pipeline-stage used-stage">Stage {usedStage}</div>,
        );
      }
    }, this);

    return formattedStages;
  },

  _pipelineFormatter(pipeline) {
    return (
      <tr key={pipeline.id}>
        <td className="pipeline-name">
          <Link to={Routes.SYSTEM.PIPELINES.PIPELINE(pipeline.id)}>{pipeline.title}</Link><br />
          {pipeline.description}
          <br />
          <MetricContainer name={`org.graylog.plugins.pipelineprocessor.ast.Pipeline.${pipeline.id}.executed`}>
            <CounterRate prefix="Throughput:" suffix="msg/s" />
          </MetricContainer>
        </td>
        <td className="stream-list">
          <PipelineConnectionsList pipeline={pipeline}
                                   connections={this.state.connections}
                                   streams={this.state.streams}
                                   streamsFormatter={this._formatConnectedStreams}
                                   noConnectionsMessage={<em>Not connected</em>} />
        </td>
        <td>{this._formatStages(pipeline, pipeline.stages)}</td>
        <td>
          <Button bsStyle="primary" bsSize="xsmall" onClick={this._deletePipeline(pipeline)}>Delete</Button>
          &nbsp;
          <LinkContainer to={Routes.SYSTEM.PIPELINES.PIPELINE(pipeline.id)}>
            <Button bsStyle="info" bsSize="xsmall">Edit</Button>
          </LinkContainer>
        </td>
      </tr>
    );
  },

  _deletePipeline(pipeline) {
    return () => {
      if (confirm(`Do you really want to delete pipeline "${pipeline.title}"? This action cannot be undone.`)) {
        PipelinesActions.delete(pipeline.id);
      }
    };
  },

  _isLoading() {
    return !this.state.pipelines || !this.state.streams || !this.state.connections;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const addNewPipelineButton = (
      <div>
        <LinkContainer to={Routes.SYSTEM.PIPELINES.PIPELINE('new')}>
          <Button bsStyle="success">Add new pipeline</Button>
        </LinkContainer>
      </div>
    );

    if (this.state.pipelines.length === 0) {
      return (
        <div>
          <StyledAlert>
            <span>There are no pipelines configured in your system. Create one to start processing your messages.</span>

            {addNewPipelineButton}
          </StyledAlert>
        </div>
      );
    }

    this.usedStages = this._calculateUsedStages(this.state.pipelines);

    const headers = ['Pipeline', 'Connected to Streams', 'Processing Timeline', 'Actions'];
    return (
      <div>
        {addNewPipelineButton}
        <DataTable id="processing-timeline"
                   className="table-hover"
                   headers={headers}
                   headerCellFormatter={this._headerCellFormatter}
                   sortByKey="title"
                   rows={this.state.pipelines}
                   dataRowFormatter={this._pipelineFormatter}
                   filterLabel="Filter pipelines"
                   filterKeys={['title']} />
      </div>
    );
  },
});

export default ProcessingTimelineComponent;
