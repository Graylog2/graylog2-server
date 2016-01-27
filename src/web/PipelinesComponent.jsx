import React, {PropTypes} from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';
import { Input, Alert } from 'react-bootstrap';

import PipelinesActions from 'PipelinesActions';
import PipelineForm from 'PipelineForm';
import Pipeline from 'Pipeline';

const PipelinesComponent = React.createClass({
  propTypes: {
    pipelines: PropTypes.array.isRequired,
  },

  _formatPipeline(pipeline) {
    return <Pipeline key={"pipeline-" + pipeline.id}
                 pipeline={pipeline}
                 user={this.props.user}
                 delete={this._delete}
                 save={this._save}
                 validatePipeline={this._validatePipeline}
    />;
  },

  _sortByTitle(pipeline1, pipeline2) {
    return pipeline1.title.localeCompare(pipeline2.title);
  },

  _save(pipeline, callback) {
    console.log(pipeline);
    if (pipeline.id) {
      PipelinesActions.update(pipeline);
    } else {
      PipelinesActions.save(pipeline);
    }
    callback();
  },

  _delete(pipeline) {
    PipelinesActions.delete(pipeline.id);
  },

  _validatePipeline(pipeline, setErrorsCb) {
    PipelinesActions.parse(pipeline, setErrorsCb);
  },

  render() {
    let pipelines;
    if (this.props.pipelines.length == 0) {
      pipelines =
        <Alert bsStyle='warning'>
          <i className="fa fa-info-circle" />&nbsp; No pipelines configured.
        </Alert>
    } else {
      pipelines = this.props.pipelines.sort(this._sortByTitle).map(this._formatPipeline);
    }

    return (
      <Row className="content">
        <Col md={12}>
          <h2>Pipelines</h2>
          <ul>
            {pipelines}
          </ul>
          <PipelineForm create
                    save={this._save}
                    validatePipeline={this._validatePipeline}
          />
        </Col>
      </Row>
    );
  },
});

export default PipelinesComponent;