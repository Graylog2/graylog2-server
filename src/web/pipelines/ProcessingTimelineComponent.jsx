import React from 'react';
import Reflux from 'reflux';
import { Alert } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import naturalSort from 'javascript-natural-sort';

import { DataTable, Spinner } from 'components/common';

import PipelinesActions from 'PipelinesActions';
import PipelinesStore from 'PipelinesStore';

import {} from './ProcessingTimelineComponent.css';

const ProcessingTimelineComponent = React.createClass({
  mixins: [Reflux.connect(PipelinesStore)],

  componentDidMount() {
    PipelinesActions.list();
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
    const style = {};
    if (header === 'Pipeline') {
      style.width = 300;
    }

    return <th style={style}>{header}</th>;
  },

  _formatStages(pipeline, stages) {
    const formattedStages = [];
    const stageNumbers = stages.map(stage => stage.stage);

    this.usedStages.forEach(usedStage => {
      if (stageNumbers.indexOf(usedStage) === -1) {
        formattedStages.push(
          <div key={`${pipeline.id}-stage${usedStage}`} className="pipeline-stage idle-stage">Idle</div>
        );
      } else {
        formattedStages.push(
          <div key={`${pipeline.id}-stage${usedStage}`} className="pipeline-stage used-stage">Stage {usedStage}</div>
        );
      }
    }, this);

    return formattedStages;
  },

  _pipelineFormatter(pipeline) {
    return (
      <tr>
        <td>
          <LinkContainer to={`/system/pipelines/${pipeline.id}`}><a>{pipeline.title}</a></LinkContainer>
        </td>
        <td>{this._formatStages(pipeline, pipeline.stages)}</td>
      </tr>
    );
  },

  render() {
    if (!this.state.pipelines) {
      return <Spinner/>;
    }

    if (this.state.pipelines.length === 0) {
      return (
        <Alert>
          There are no pipelines configured in your system. Create one to start processing your messages.
        </Alert>
      );
    }

    this.usedStages = this._calculateUsedStages(this.state.pipelines);

    const headers = ['Pipeline', 'ProcessingTimeline'];
    return (
      <DataTable id="processing-timeline"
                 className="table-hover"
                 headers={headers}
                 headerCellFormatter={this._headerCellFormatter}
                 sortByKey={'title'}
                 rows={this.state.pipelines}
                 dataRowFormatter={this._pipelineFormatter}
                 filterLabel=""
                 filterKeys={[]}/>
    );
  },
});

export default ProcessingTimelineComponent;
