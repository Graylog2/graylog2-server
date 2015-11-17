import React, {PropTypes} from 'react';
import Reflux from 'reflux';
import {Row, Col, Input} from 'react-bootstrap';

import {ClipboardButton, Spinner} from 'components/common';
import Version from 'util/Version';

import ExtractorsActions from 'actions/extractors/ExtractorsActions';
import ExtractorsStore from 'stores/extractors/ExtractorsStore';

const ExportExtractors = React.createClass({
  propTypes: {
    input: PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(ExtractorsStore), Reflux.ListenerMethods],
  componentDidMount() {
    ExtractorsActions.list.triggerPromise(this.props.input.input_id);
  },
  _isLoading() {
    return !this.state.extractors;
  },
  render() {
    if (this._isLoading()) {
      return <Spinner/>;
    }

    const extractorsExportObject = {
      extractors: this.state.extractors,
      version: Version.getFullVersion(),
    };

    const formattedJSON = JSON.stringify(extractorsExportObject, null, 2);
    return (
      <Row className="content">
        <Col md={12}>
          <Row>
            <Col md={8}>
              <h2>Extractors JSON</h2>
            </Col>
            <Col md={4}>
              <ClipboardButton title="Copy extractors" className="pull-right" target="#extractor-export-textarea"/>
            </Col>
          </Row>
          <Row>
            <Col md={12}>
              <Input type="textarea" id="extractor-export-textarea" rows={30} defaultValue={formattedJSON}/>
            </Col>
          </Row>
        </Col>
      </Row>
    );
  },
});

export default ExportExtractors;
