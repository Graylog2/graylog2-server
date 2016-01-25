import React, {PropTypes} from 'react';
import {Row, Col, Button} from 'react-bootstrap';

import AceEditor from 'react-ace';
import brace from 'brace';

import 'brace/mode/text';
import 'brace/theme/chrome';

import RuleForm from 'RuleForm';

const Rule = React.createClass({
  propTypes: {
    rule: PropTypes.object.isRequired,
  },

  _delete() {
    this.props.delete(this.props.rule);
  },

  render() {
    return <li>
      <h2>{this.props.rule.title}</h2>
      <Row>
        <Col md={3}>{this.props.rule.description}</Col>
        <Col md={8}>
          <label>Rule source</label>
          <div style={{border: "1px solid lightgray", borderRadius: 5, width: 502}}>
            <AceEditor
              mode="text"
              theme="chrome"
              name={"rule-source-show-" + this.props.rule.id}
              fontSize={11}
              height="14em"
              width="500px"
              readonly
              editorProps={{ $blockScrolling: "Infinity" }}
              value={this.props.rule.source}
            />
          </div>
        </Col>
        <Col md={1}>
          <RuleForm rule={this.props.rule} save={this.props.save} validateName={this.props.validateName}/>
          <Button style={{marginRight: 5}} bsStyle="primary" bsSize="xs"
                  onClick={this._delete}>
            Delete
          </Button>
        </Col>
      </Row>
    </li>;
  }
});

export default Rule;