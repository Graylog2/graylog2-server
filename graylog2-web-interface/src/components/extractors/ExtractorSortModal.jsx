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

import { Row, Col, Modal, Button } from 'components/graylog';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import SortableList from 'components/common/SortableList';
import ActionsProvider from 'injection/ActionsProvider';

const ExtractorsActions = ActionsProvider.getActions('Extractors');

class ExtractorSortModal extends React.Component {
  static propTypes = {
    input: PropTypes.object.isRequired,
    extractors: PropTypes.array.isRequired,
  };

  constructor(props) {
    super(props);

    this.state = {
      sortedExtractors: props.extractors,
    };
  }

  open = () => {
    this.modal.open();
  };

  close = () => {
    this.modal.close();
  };

  _updateSorting = (newSorting) => {
    this.setState({
      sortedExtractors: newSorting,
    });
  };

  _saveSorting = () => {
    const { input } = this.props;
    const { sortedExtractors } = this.state;

    if (!sortedExtractors) {
      this.close();
    }

    const promise = ExtractorsActions.order.triggerPromise(input.id, sortedExtractors);

    promise.then(() => this.close());
  };

  render() {
    const { sortedExtractors } = this.state;
    const { input } = this.props;

    return (
      <BootstrapModalWrapper ref={(modal) => { this.modal = modal; }}>
        <Modal.Header closeButton>
          <Modal.Title>
            <span>Sort extractors for <em>{input.title}</em></span>
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p>Drag and drop the extractors on the list to change the order in which they will be applied.</p>
          <Row className="row-sm">
            <Col md={12}>
              <SortableList items={sortedExtractors} onSortChange={this._updateSorting} displayOverlayInPortal />
            </Col>
          </Row>
        </Modal.Body>
        <Modal.Footer>
          <Button type="button" onClick={this.close}>Close</Button>
          <Button type="button" bsStyle="info" onClick={this._saveSorting}>Save</Button>
        </Modal.Footer>
      </BootstrapModalWrapper>
    );
  }
}

export default ExtractorSortModal;
