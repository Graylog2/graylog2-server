import React from 'react';
import { Row, Col } from 'react-bootstrap';

import PipelineDetails from './PipelineDetails';

import Routes from 'routing/Routes';

const NewPipeline = React.createClass({
  propTypes: {
    onChange: React.PropTypes.func.isRequired,
    history: React.PropTypes.object.isRequired,
  },

  _onChange(newPipeline) {
    this.props.onChange(newPipeline, this._goToPipeline);
  },

  _goToPipeline(pipeline) {
    this.props.history.pushState(null, Routes.pluginRoute('SYSTEM_PIPELINES_PIPELINEID')(pipeline.id));
  },

  _goBack() {
    this.props.history.goBack();
  },

  render() {
    return (
      <Row>
        <Col md={6}>
          <p>
            Give a name and description to the new pipeline. You can add stages to it when you save the changes.
          </p>
          <PipelineDetails create onChange={this._onChange} onCancel={this._goBack} />
        </Col>
      </Row>
    );
  },
});

export default NewPipeline;
