import React, {PropTypes} from 'react';
import { Row, Col } from 'react-bootstrap';

import { EntityList } from 'components/common';
import Stage from './Stage';
import StageForm from './StageForm';

const Pipeline = React.createClass({
  propTypes: {
    pipeline: PropTypes.object.isRequired,
    onStagesChange: PropTypes.func.isRequired,
  },

  _saveStage(stage, callback) {
    const newStages = this.props.pipeline.stages.slice();
    newStages.push(stage);
    this.props.onStagesChange(newStages, callback);
  },

  _updateStage(prevStage) {
    return (stage, callback) => {
      const newStages = this.props.pipeline.stages.filter(s => s.stage !== prevStage.stage);
      newStages.push(stage);
      this.props.onStagesChange(newStages, callback);
    };
  },

  _deleteStage(stage) {
    return () => {
      if (confirm(`You are about to delete stage ${stage.stage}, are you sure you want to proceed?`)) {
        const newStages = this.props.pipeline.stages.filter(s => s.stage !== stage.stage);
        this.props.onStagesChange(newStages);
      }
    };
  },

  _formatStage(stage, maxStage) {
    return (
      <Stage key={`stage-${stage.stage}`} stage={stage} isLastStage={stage.stage === maxStage}
             onUpdate={this._updateStage(stage)} onDelete={this._deleteStage(stage)}/>
    );
  },

  render() {
    const maxStage = this.props.pipeline.stages.reduce((max, currentStage) => Math.max(max, currentStage.stage), -Infinity);
    const formattedStages = this.props.pipeline.stages
      .sort((s1, s2) => s1.stage - s2.stage)
      .map(stage => this._formatStage(stage, maxStage));

    return (
      <div>
        <Row>
          <Col md={12}>
            <div className="pull-right">
              <StageForm create save={this._saveStage}/>
            </div>
            <h2>Description</h2>
            <p style={{marginTop: 5}}>{this.props.pipeline.description}</p>
          </Col>
        </Row>
        <EntityList bsNoItemsStyle="info" noItemsText="There are no rules on this stage." items={formattedStages}/>
      </div>
    );
  },
});

export default Pipeline;
