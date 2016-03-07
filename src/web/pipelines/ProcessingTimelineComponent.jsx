import React from 'react';
import Reflux from 'reflux';
import { Alert, Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import naturalSort from 'javascript-natural-sort';

import { DataTable, Spinner } from 'components/common';
import PipelineForm from './PipelineForm';

import PipelinesActions from 'pipelines/PipelinesActions';
import PipelinesStore from 'pipelines/PipelinesStore';

import ObjectUtils from 'util/ObjectUtils';
import SourceGenerator from 'logic/SourceGenerator';

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
    let className;
    if (header === 'Actions') {
      className = 'actions';
    }

    return <th className={className}>{header}</th>;
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
      <tr key={pipeline.id}>
        <td className="pipeline-name">
          <LinkContainer to={`/system/pipelines/${pipeline.id}`}><a>{pipeline.title}</a></LinkContainer><br />
          {pipeline.description}
        </td>
        <td>{this._formatStages(pipeline, pipeline.stages)}</td>
        <td>
          <Button bsStyle="primary" bsSize="xsmall" onClick={this._deletePipeline(pipeline)}>Delete</Button>
          &nbsp;
          <PipelineForm pipeline={pipeline} save={this._savePipeline}/>
        </td>
      </tr>
    );
  },

  _savePipeline(pipeline, callback) {
    const requestPipeline = ObjectUtils.clone(pipeline);
    requestPipeline.source = SourceGenerator.generatePipeline(pipeline);
    if (requestPipeline.id) {
      PipelinesActions.update(requestPipeline);
    } else {
      PipelinesActions.save(requestPipeline);
    }
    callback();
  },

  _deletePipeline(pipeline) {
    return () => {
      if (confirm(`Do you really want to delete pipeline "${pipeline.title}"? This action cannot be undone.`)) {
        PipelinesActions.delete(pipeline.id);
      }
    };
  },

  render() {
    if (!this.state.pipelines) {
      return <Spinner />;
    }

    if (this.state.pipelines.length === 0) {
      return (
        <div>
          <div className="text-right"><PipelineForm create save={this._savePipeline} /></div>
          <Alert>
            There are no pipelines configured in your system. Create one to start processing your messages.
          </Alert>
        </div>
      );
    }

    this.usedStages = this._calculateUsedStages(this.state.pipelines);

    const headers = ['Pipeline', 'ProcessingTimeline', 'Actions'];
    return (
      <div>
        <div className="pull-right"><PipelineForm create save={this._savePipeline}/></div>
        <DataTable id="processing-timeline"
                   className="table-hover"
                   headers={headers}
                   headerCellFormatter={this._headerCellFormatter}
                   sortByKey={'title'}
                   rows={this.state.pipelines}
                   dataRowFormatter={this._pipelineFormatter}
                   filterLabel="Filter pipelines"
                   filterKeys={['title']} />
      </div>
    );
  },
});

export default ProcessingTimelineComponent;
