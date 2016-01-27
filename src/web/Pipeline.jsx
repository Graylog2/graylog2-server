import React, {PropTypes} from 'react';
import {Row, Col, Button} from 'react-bootstrap';

import AceEditor from 'react-ace';
import brace from 'brace';

import 'brace/mode/text';
import 'brace/theme/chrome';

import PipelineForm from 'PipelineForm';

const Pipeline = React.createClass({
  propTypes: {
    pipeline: PropTypes.object.isRequired,
  },

  _delete() {
    this.props.delete(this.props.pipeline);
  },

  render() {
    return <li>
      <h2>{this.props.pipeline.title}</h2>
      <Row>
        <Col md={3}>{this.props.pipeline.description}</Col>
        <Col md={8}>
          <label>Pipeline source</label>
          <div style={{border: "1px solid lightgray", borderRadius: 5, width: 502}}>
            <AceEditor
              mode="text"
              theme="chrome"
              name={"pipeline-source-show-" + this.props.pipeline.id}
              fontSize={11}
              height="14em"
              width="500px"
              readonly
              editorProps={{ $blockScrolling: "Infinity" }}
              value={this.props.pipeline.source}
            />
          </div>
        </Col>
        <Col md={1}>
          <PipelineForm pipeline={this.props.pipeline} save={this.props.save}
                    validatePipeline={this.props.validatePipeline}/>
          <Button style={{marginRight: 5}} bsStyle="primary" bsSize="xs"
                  onClick={this._delete}>
            Delete
          </Button>
        </Col>
      </Row>
    </li>;
  }
});

export default Pipeline;