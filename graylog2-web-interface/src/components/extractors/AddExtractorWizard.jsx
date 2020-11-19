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

import { Row, Col, Button } from 'components/graylog';
import LoaderTabs from 'components/messageloaders/LoaderTabs';
import MessageFieldExtractorActions from 'components/search/MessageFieldExtractorActions';

class AddExtractorWizard extends React.Component {
  static propTypes = {
    inputId: PropTypes.string,
  };

  state = {
    showExtractorForm: false,
  };

  _showAddExtractorForm = () => {
    this.setState({ showExtractorForm: !this.state.showExtractorForm });
  };

  render() {
    let extractorForm;

    if (this.state.showExtractorForm) {
      // Components using this component, will give it a proper fieldName and message
      const extractorFieldActions = <MessageFieldExtractorActions fieldName="" message={{}} />;

      extractorForm = (
        <div className="stream-loader">
          <LoaderTabs selectedInputId={this.props.inputId} customFieldActions={extractorFieldActions} />
        </div>
      );
    }

    return (
      <Row className="content">
        <Col md={12}>
          <h2 style={{ marginBottom: 5 }}>Add extractor</h2>

          <p>
            Start by loading a message to have an example to work on. You can decide whether to load a recent message
            received by this input, or manually select a message giving its ID.
          </p>
          <p>
            <Button bsStyle="info" bsSize="small" onClick={this._showAddExtractorForm} disabled={this.state.showExtractorForm}>
              Get started
            </Button>
          </p>

          {extractorForm}
        </Col>
      </Row>
    );
  }
}

export default AddExtractorWizard;
