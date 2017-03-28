import React, { PropTypes } from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';

import { Input } from 'components/bootstrap';
import { ClipboardButton, Spinner } from 'components/common';
import Version from 'util/Version';

import ActionsProvider from 'injection/ActionsProvider';
const ExtractorsActions = ActionsProvider.getActions('Extractors');

import StoreProvider from 'injection/StoreProvider';
const ExtractorsStore = StoreProvider.getStore('Extractors');

const ExportExtractors = React.createClass({
  propTypes: {
    input: PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(ExtractorsStore), Reflux.ListenerMethods],
  componentDidMount() {
    ExtractorsActions.list.triggerPromise(this.props.input.id);
  },
  _isLoading() {
    return !this.state.extractors;
  },
  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const extractorsExportObject = {
      extractors: this.state.extractors.map((extractor) => {
        const copy = {};

        // Create Graylog 1.x compatible export format.
        // TODO: This should be done on the server.
        Object.keys(extractor).forEach((key) => {
          switch (key) {
            case 'type':
              // The import expects "extractor_type", not "type".
              copy.extractor_type = extractor[key];
              break;
            case 'id':
            case 'metrics':
            case 'creator_user_id':
            case 'exceptions':
            case 'converter_exceptions':
              break;
            default:
              copy[key] = extractor[key];
          }
        });

        return copy;
      }),
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
              <ClipboardButton title="Copy extractors" className="pull-right" target="#extractor-export-textarea" />
            </Col>
          </Row>
          <Row>
            <Col md={12}>
              <Input type="textarea" id="extractor-export-textarea" rows={30} defaultValue={formattedJSON} />
            </Col>
          </Row>
        </Col>
      </Row>
    );
  },
});

export default ExportExtractors;
