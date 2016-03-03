import React, {PropTypes} from 'react';
import Reflux from 'reflux';
import { Row, Col, Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import { PageHeader, Spinner } from 'components/common';
import Pipeline from './Pipeline';

import SourceGenerator from 'logic/SourceGenerator';
import ObjectUtils from 'util/ObjectUtils';
import PipelinesActions from 'PipelinesActions';
import PipelinesStore from 'PipelinesStore';
import RulesStore from 'rules/RulesStore';

function filterPipeline(state) {
  return state.pipelines ? state.pipelines.filter(p => p.id === this.props.params.pipelineId)[0] : undefined;
}

const PipelineDetailsPage = React.createClass({
  propTypes: {
    params: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connectFilter(PipelinesStore, 'pipeline', filterPipeline)],

  componentDidMount() {
    PipelinesActions.get(this.props.params.pipelineId);
    RulesStore.list();
  },

  _onStagesChange(newStages, callback) {
    const newPipeline = ObjectUtils.clone(this.state.pipeline);
    newPipeline.stages = newStages;
    const pipelineSource = SourceGenerator.generatePipeline(newPipeline);
    newPipeline.source = pipelineSource;
    PipelinesActions.update(newPipeline);
    callback();
  },

  render() {
    if (!this.state.pipeline) {
      return <Spinner/>;
    }

    return (
      <div>
        <PageHeader title={<span>Pipeline "{this.state.pipeline.title}"</span>} titleSize={9} buttonSize={3}>
          <span>
            Pipelines let you transform and process messages coming from streams. Pipelines consist of stages where{' '}
            rules are evaluated and applied. Messages can go through one or more stages.
          </span>
          <span>
            After each stage is completed, you can decide if messages matching all or one of the rules continue to the next stage.
          </span>

          <span>
            <LinkContainer to={'/system/pipelines/overview'}>
              <Button bsStyle="info">Manage pipelines</Button>
            </LinkContainer>
            {' '}
            <LinkContainer to={'/system/pipelines/rules'}>
              <Button bsStyle="info">Manage rules</Button>
            </LinkContainer>
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <Pipeline pipeline={this.state.pipeline} rules={this.state.rules} onStagesChange={this._onStagesChange}/>
          </Col>
        </Row>
      </div>
    );
  },
});

export default PipelineDetailsPage;
