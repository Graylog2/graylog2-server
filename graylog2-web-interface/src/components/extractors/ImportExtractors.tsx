import React from 'react';

import { Row, Col, Button, Input } from 'components/bootstrap';
import UserNotification from 'util/UserNotification';
import { ExtractorsActions } from 'stores/extractors/ExtractorsStore';

type ImportExtractorsProps = {
  input: any;
};

class ImportExtractors extends React.Component<ImportExtractorsProps, {
  [key: string]: any;
}> {
  _onSubmit = (event) => {
    event.preventDefault();

    try {
      const parsedExtractors = JSON.parse(this.extractorsInput.getValue());
      const { extractors } = parsedExtractors;

      ExtractorsActions.import(this.props.input.id, extractors);
    } catch (error) {
      UserNotification.error(`There was an error while parsing extractors. Are they in JSON format? ${error}`,
        'Could not import extractors');
    }
  };

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
                <Input type="textarea" ref={(extractorsInput) => { this.extractorsInput = extractorsInput; }} id="extractor-export-textarea" rows={30} />
                <Button type="submit" bsStyle="success">Add extractors to input</Button>
              </form>
            </Col>
          </Row>
        </Col>
      </Row>
    );
  }
}

export default ImportExtractors;
