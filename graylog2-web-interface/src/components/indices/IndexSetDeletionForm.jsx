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
import naturalSort from 'javascript-natural-sort';

import { Alert, Row, Col } from 'components/graylog';
import { Input } from 'components/bootstrap';
import { Spinner } from 'components/common';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import CombinedProvider from 'injection/CombinedProvider';

const { StreamsStore } = CombinedProvider.get('Streams');

class IndexSetDeletionForm extends React.Component {
  static propTypes = {
    indexSet: PropTypes.object.isRequired,
    onDelete: PropTypes.func.isRequired,
  };

  state = {
    assignedStreams: undefined,
    deleteIndices: true,
  };

  forms = {};

  _onModalOpen = () => {
    StreamsStore.load((streams) => {
      const assignedStreams = [];

      streams.forEach((stream) => {
        if (stream.index_set_id === this.props.indexSet.id) {
          assignedStreams.push({ id: stream.id, title: stream.title });
        }
      });

      this.setState({ assignedStreams: assignedStreams });
    });
  };

  _onRemoveClick = (e) => {
    this.setState({ deleteIndices: e.target.checked });
  };

  open = () => {
    this.forms[`index-set-deletion-modal-${this.props.indexSet.id}`].open();
  };

  close = () => {
    this.forms[`index-set-deletion-modal-${this.props.indexSet.id}`].close();
  };

  _isLoading = () => {
    return !this.state.assignedStreams;
  };

  _isDeletable = () => {
    return !this._isLoading() && this.state.assignedStreams.length < 1 && !this.props.indexSet.default;
  };

  _modalContent = () => {
    if (this._isLoading()) {
      return <Spinner text="Loading assigned streams..." />;
    }

    if (this.props.indexSet.default) {
      return (
        <Row>
          <Col md={12}>
            <Alert bsStyle="danger">
              Unable to delete the index set because it is the default index set!
            </Alert>
          </Col>
        </Row>
      );
    }

    if (!this._isDeletable()) {
      const assignedStreams = this.state.assignedStreams
        .sort((s1, s2) => naturalSort(s1.title, s2.title))
        .map((stream) => <li key={`stream-id-${stream.id}`}>{stream.title}</li>);

      return (
        <div>
          <Row>
            <Col md={12}>
              <Alert bsStyle="danger">
                Unable to delete the index set because it has assigned streams. Remove stream assignments to be able to delete this index set.
              </Alert>
            </Col>
          </Row>
          <Row>
            <Col md={12}>
              <h4>Assigned streams:</h4>
              <ul>
                {assignedStreams}
              </ul>
            </Col>
          </Row>
        </div>
      );
    }

    return (
      <Row>
        <Col md={12}>
          <Input id="remove-data-checkbox"
                 type="checkbox"
                 label="Remove all data for this index set?"
                 help={<span>All indices related to this index set will be deleted from Elasticsearch.</span>}
                 checked={this.state.deleteIndices}
                 onChange={this._onRemoveClick} />
        </Col>
      </Row>
    );
  };

  _onDelete = (e) => {
    e.preventDefault();

    if (this._isDeletable()) {
      this.props.onDelete(this.props.indexSet, this.state.deleteIndices);
    }
  };

  render() {
    return (
      <BootstrapModalForm ref={(elem) => { this.forms[`index-set-deletion-modal-${this.props.indexSet.id}`] = elem; }}
                          title={`Delete index set "${this.props.indexSet.title}"?`}
                          onModalOpen={this._onModalOpen}
                          onSubmitForm={this._onDelete}
                          submitButtonText="Delete"
                          submitButtonDisabled={!this._isDeletable()}>
        {this._modalContent()}
      </BootstrapModalForm>
    );
  }
}

export default IndexSetDeletionForm;
