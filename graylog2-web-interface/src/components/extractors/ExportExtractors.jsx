/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { Row, Col } from 'components/graylog';
import { Input } from 'components/bootstrap';
import { ClipboardButton, Spinner } from 'components/common';
import Version from 'util/Version';
import ActionsProvider from 'injection/ActionsProvider';
import StoreProvider from 'injection/StoreProvider';

const ExtractorsActions = ActionsProvider.getActions('Extractors');
const ExtractorsStore = StoreProvider.getStore('Extractors');

const ExportExtractors = createReactClass({
  displayName: 'ExportExtractors',

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
