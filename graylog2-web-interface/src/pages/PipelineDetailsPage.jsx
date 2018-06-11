import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Row, Col, Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import Pipeline from 'components/pipelines/Pipeline';
import NewPipeline from 'components/pipelines/NewPipeline';

import SourceGenerator from 'logic/pipelines/SourceGenerator';
import ObjectUtils from 'util/ObjectUtils';
import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';

const { PipelinesStore, PipelinesActions } = CombinedProvider.get('Pipelines');
const { RulesStore } = CombinedProvider.get('Rules');
const { PipelineConnectionsStore, PipelineConnectionsActions } = CombinedProvider.get('PipelineConnections');
const { StreamsStore } = CombinedProvider.get('Streams');

function filterPipeline(state) {
  return state.pipelines ? state.pipelines.filter(p => p.id === this.props.params.pipelineId)[0] : undefined;
}

function filterConnections(state) {
  if (!state.connections) {
    return undefined;
  }
  return state.connections.filter(c => c.pipeline_ids && c.pipeline_ids.includes(this.props.params.pipelineId));
}

const PipelineDetailsPage = createReactClass({
  displayName: 'PipelineDetailsPage',

  propTypes: {
    params: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connectFilter(PipelinesStore, 'pipeline', filterPipeline), Reflux.connectFilter(PipelineConnectionsStore, 'connections', filterConnections)],

  componentDidMount() {
    if (!this._isNewPipeline(this.props.params.pipelineId)) {
      PipelinesActions.get(this.props.params.pipelineId);
    }
    RulesStore.list();
    PipelineConnectionsActions.list();

    StreamsStore.listStreams().then((streams) => {
      this.setState({ streams });
    });
  },

  componentWillReceiveProps(nextProps) {
    if (!this._isNewPipeline(nextProps.params.pipelineId)) {
      PipelinesActions.get(nextProps.params.pipelineId);
    }
  },

  _onConnectionsChange(updatedConnections, callback) {
    PipelineConnectionsActions.connectToPipeline(updatedConnections);
    callback();
  },

  _onStagesChange(newStages, callback) {
    const newPipeline = ObjectUtils.clone(this.state.pipeline);
    newPipeline.stages = newStages;
    const pipelineSource = SourceGenerator.generatePipeline(newPipeline);
    newPipeline.source = pipelineSource;
    PipelinesActions.update(newPipeline);
    if (typeof callback === 'function') {
      callback();
    }
  },

  _savePipeline(pipeline, callback) {
    const requestPipeline = ObjectUtils.clone(pipeline);
    requestPipeline.source = SourceGenerator.generatePipeline(pipeline);
    let promise;
    if (requestPipeline.id) {
      promise = PipelinesActions.update(requestPipeline);
    } else {
      promise = PipelinesActions.save(requestPipeline);
    }

    promise.then(p => callback(p));
  },

  _isNewPipeline(pipelineId) {
    return pipelineId === 'new';
  },

  _isLoading() {
    return !this._isNewPipeline(this.props.params.pipelineId) && (!this.state.pipeline || !this.state.connections || !this.state.streams);
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    let title;
    if (this._isNewPipeline(this.props.params.pipelineId)) {
      title = 'New pipeline';
    } else {
      title = <span>Pipeline <em>{this.state.pipeline.title}</em></span>;
    }

    let content;
    if (this._isNewPipeline(this.props.params.pipelineId)) {
      content = <NewPipeline onChange={this._savePipeline} />;
    } else {
      content = (
        <Pipeline pipeline={this.state.pipeline}
                  connections={this.state.connections}
                  streams={this.state.streams}
                  rules={this.state.rules}
                  onConnectionsChange={this._onConnectionsChange}
                  onStagesChange={this._onStagesChange}
                  onPipelineChange={this._savePipeline} />
      );
    }

    const pageTitle = (this._isNewPipeline(this.props.params.pipelineId) ? 'New pipeline' : `Pipeline ${this.state.pipeline.title}`);

    return (
      <DocumentTitle title={pageTitle}>
        <div>
          <PageHeader title={title}>
            <span>
              Pipelines let you transform and process messages coming from streams. Pipelines consist of stages where
              rules are evaluated and applied. Messages can go through one or more stages.
            </span>
            <span>
              After each stage is completed, you can decide if messages matching all or one of the rules continue to
              the next stage.
            </span>

            <span>
              <LinkContainer to={Routes.SYSTEM.PIPELINES.OVERVIEW}>
                <Button bsStyle="info" className="active">Manage pipelines</Button>
              </LinkContainer>
              &nbsp;
              <LinkContainer to={Routes.SYSTEM.PIPELINES.RULES}>
                <Button bsStyle="info">Manage rules</Button>
              </LinkContainer>
              &nbsp;
              <LinkContainer to={Routes.SYSTEM.PIPELINES.SIMULATOR}>
                <Button bsStyle="info">Simulator</Button>
              </LinkContainer>
            </span>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              {content}
            </Col>
          </Row>
        </div>
      </DocumentTitle>
    );
  },
});

export default PipelineDetailsPage;
