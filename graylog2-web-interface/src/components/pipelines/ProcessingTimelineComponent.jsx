import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Alert, Button, Row, Col } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import { Link } from 'react-router';
import naturalSort from 'javascript-natural-sort';

import { SearchForm, PaginatedList, DataTable, Spinner } from 'components/common';
import { MetricContainer, CounterRate } from 'components/metrics';

import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';

import PipelineConnectionsList from './PipelineConnectionsList';

const { PipelinesStore, PipelinesActions } = CombinedProvider.get('Pipelines');
const { PipelineConnectionsStore, PipelineConnectionsActions } = CombinedProvider.get('PipelineConnections');
const { StreamsStore } = CombinedProvider.get('Streams');

const ProcessingTimelineComponent = createReactClass({
  displayName: 'ProcessingTimelineComponent',
  mixins: [Reflux.connect(PipelinesStore), Reflux.connect(PipelineConnectionsStore)],

  componentDidMount() {
    this.style.use();
    this.loadData();
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

  loadData(callback) {
    const { page, perPage, query } = this.state.pagination;
    PipelinesActions.listPage(page, perPage, query).then(() => {
      if (callback) {
        callback();
      }
    });
  },

  _onPageChange(newPage, newPerPage) {
    const pagination = this.state.pagination;
    const newPagination = Object.assign(pagination, {
      page: newPage,
      perPage: newPerPage,
    });
    this.setState({ pagination, newPagination }, this.loadData);
  },

  _onSearch(query, resetLoadingCallback) {
    const pagination = this.state.pagination;
    const newPagination = Object.assign(pagination, { query: query });
    this.setState({ pagination, newPagination }, () => this.loadData(resetLoadingCallback));
  },

  _onReset() {
    const pagination = this.state.pagination;
    const newPagination = Object.assign(pagination, { query: '' });
    this.setState({ pagination, newPagination }, this.loadData);
  },

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
      <div className={'pipeline-create-button'}>
        <LinkContainer to={Routes.SYSTEM.PIPELINES.PIPELINE('new')}>
          <Button bsStyle="success">Add new pipeline</Button>
        </LinkContainer>
      </div>
    );

    const noDataText = (
      <div>
        <Alert>
          There were no pipelines found. Change your query or create one to start processing your messages.
        </Alert>
      </div>
    );

    this.usedStages = this._calculateUsedStages(this.state.pipelines);

    const headers = ['Pipeline', 'Connected to Streams', 'Processing Timeline', 'Actions'];
    return (
      <div>
        <Row className="row-sm">
          <Col md={2}>
            <SearchForm onSearch={this._onSearch} onReset={this._onReset} useLoadingState />
          </Col>
          <Col>
            {addNewPipelineButton}
          </Col>
        </Row>
        <Row>
          <Col md={12}>
            <PaginatedList onChange={this._onPageChange} totalItems={this.state.pagination.total}>
              <br />
              <br />
              <DataTable id="processing-timeline"
                         className="table-hover"
                         headers={headers}
                         headerCellFormatter={this._headerCellFormatter}
                         sortByKey={'title'}
                         rows={this.state.pipelines}
                         dataRowFormatter={this._pipelineFormatter}
                         noDataText={noDataText}
                         filterKeys={[]} />
            </PaginatedList>
          </Col>
        </Row>
      </div>
    );
  },
});

export default ProcessingTimelineComponent;
