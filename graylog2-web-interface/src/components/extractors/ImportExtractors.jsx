import React, { PropTypes } from 'react';
import { Row, Col, Button } from 'react-bootstrap';

import { Input } from 'components/bootstrap';
import ActionsProvider from 'injection/ActionsProvider';
const ExtractorsActions = ActionsProvider.getActions('Extractors');

import UserNotification from 'util/UserNotification';

const ImportExtractors = React.createClass({
  propTypes: {
    input: PropTypes.object.isRequired,
  },
  _onSubmit(event) {
    event.preventDefault();
    try {
      const parsedExtractors = JSON.parse(this.refs.extractorsInput.getValue());
      const extractors = parsedExtractors.extractors;
      ExtractorsActions.import(this.props.input.id, extractors);
    } catch (error) {
      UserNotification.error(`There was an error while parsing extractors. Are they in JSON format? ${error}`,
        'Could not import extractors');
    }
  },
  render() {
    return (
      <Row className="content">
        <Col md={12}>
          <Row>
            <Col md={12}>
              <h2>Extractors JSON</h2>
            </Col>
          </Row>
          <Row>
            <Col md={12}>
              <form onSubmit={this._onSubmit}>
                <Input type="textarea" ref="extractorsInput" id="extractor-export-textarea" rows={30} />
                <Button type="submit" bsStyle="success">Add extractors to input</Button>
              </form>
            </Col>
          </Row>
        </Col>
      </Row>
    );
  },
});

export default ImportExtractors;
