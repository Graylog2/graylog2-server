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
import React from 'react';

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import { Spinner } from 'components/common';
import { Alert, Row, Col, Input } from 'components/bootstrap';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import { fetchStreams } from 'api/streams';
import i18n from 'i18n';

type IndexSetDeletionFormProps = {
  indexSet: any;
  onDelete: (...args: any[]) => void;
};

class IndexSetDeletionForm extends React.Component<
  IndexSetDeletionFormProps,
  {
    [key: string]: any;
  }
> {
  constructor(props) {
    super(props);

    this.state = {
      showModal: false,
      assignedStreams: undefined,
      deleteIndices: true,
    };
  }

  _onModalOpen = () => {
    fetchStreams().then((streams) => {
      const assignedStreams = [];

      streams.forEach((stream) => {
        if (stream.index_set_id === this.props.indexSet.id) {
          assignedStreams.push({ id: stream.id, title: stream.title });
        }
      });

      this.setState({ assignedStreams: assignedStreams });
    });
  };

  // eslint-disable-next-line react/no-unused-class-component-methods
  open = () => {
    this.setState({ showModal: true }, this._onModalOpen);
  };

  close = () => {
    this.setState({ showModal: false });
  };

  _onRemoveClick = (e) => {
    this.setState({ deleteIndices: e.target.checked });
  };

  _isLoading = () => !this.state.assignedStreams;

  _isDeletable = () => !this._isLoading() && this.state.assignedStreams.length < 1 && !this.props.indexSet.default;

  _modalContent = () => {
    if (this._isLoading()) {
      return <Spinner text={i18n.t('deletion_form.loading_streams', { ns: 'indexsets' })} />;
    }

    if (this.props.indexSet.default) {
      return (
        <Row>
          <Col md={12}>
            <Alert bsStyle="danger">{i18n.t('deletion_form.default_index_set', { ns: 'indexsets' })}</Alert>
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
              <Alert bsStyle="danger">{i18n.t('deletion_form.has_streams', { ns: 'indexsets' })}</Alert>
            </Col>
          </Row>
          <Row>
            <Col md={12}>
              <h4>{i18n.t('deletion_form.assigned_streams', { ns: 'indexsets' })}</h4>
              <ul>{assignedStreams}</ul>
            </Col>
          </Row>
        </div>
      );
    }

    return (
      <Row>
        <Col md={12}>
          <Input
            id="remove-data-checkbox"
            type="checkbox"
            label={i18n.t('deletion_form.remove_data_label', { ns: 'indexsets' })}
            help={<span>{i18n.t('deletion_form.remove_data_help', { ns: 'indexsets' })}</span>}
            checked={this.state.deleteIndices}
            onChange={this._onRemoveClick}
          />
        </Col>
      </Row>
    );
  };

  _onDelete = (e) => {
    e.preventDefault();

    if (this._isDeletable()) {
      this.props.onDelete(this.props.indexSet, this.state.deleteIndices);
      this.close();
    }
  };

  render() {
    return (
      <BootstrapModalForm
        show={this.state.showModal}
        title={i18n.t('deletion_form.title', { ns: 'indexsets', title: this.props.indexSet.title })}
        data-telemetry-title="Delete index set"
        onCancel={this.close}
        onSubmitForm={this._onDelete}
        submitButtonText={i18n.t('deletion_form.delete', { ns: 'indexsets' })}
        submitButtonDisabled={!this._isDeletable()}>
        {this._modalContent()}
      </BootstrapModalForm>
    );
  }
}

export default IndexSetDeletionForm;
