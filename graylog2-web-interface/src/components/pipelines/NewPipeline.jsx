import PropTypes from 'prop-types';
import React from 'react';
import { Row, Col } from 'react-bootstrap';

import history from 'util/History';
import PipelineDetails from './PipelineDetails';

import Routes from 'routing/Routes';

const NewPipeline = React.createClass({
  propTypes: {
    onChange: PropTypes.func.isRequired,
  },

  _onChange(newPipeline) {
    this.props.onChange(newPipeline, this._goToPipeline);
  },

  _goToPipeline(pipeline) {
    history.push(Routes.pluginRoute('SYSTEM_PIPELINES_PIPELINEID')(pipeline.id));
  },

  _goBack() {
    history.goBack();
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
