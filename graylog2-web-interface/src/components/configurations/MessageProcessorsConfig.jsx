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
import naturalSort from 'javascript-natural-sort';

import { Button, Alert, Table } from 'components/graylog';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import { IfPermitted, SortableList } from 'components/common';
import ObjectUtils from 'util/ObjectUtils';

const MessageProcessorsConfig = createReactClass({
  displayName: 'MessageProcessorsConfig',

  propTypes: {
    config: PropTypes.object.isRequired,
    updateConfig: PropTypes.func.isRequired,
  },

  getDefaultProps() {
    return {
      config: {
        disabled_processors: [],
        processor_order: [],
      },
    };
  },

  getInitialState() {
    return {
      config: {
        disabled_processors: this.props.config.disabled_processors,
        processor_order: this.props.config.processor_order,
      },
    };
  },

  inputs: {},

  _openModal() {
    this.configModal.open();
  },

  _closeModal() {
    this.configModal.close();
  },

  _saveConfig() {
    if (!this._hasNoActiveProcessor()) {
      this.props.updateConfig(this.state.config).then(() => {
        this._closeModal();
      });
    }
  },

  _resetConfig() {
    // Reset to initial state when the modal is closed without saving.
    this.setState(this.getInitialState());
  },

  _updateSorting(newSorting) {
    const update = ObjectUtils.clone(this.state.config);

    update.processor_order = newSorting.map((item) => {
      return { class_name: item.id, name: item.title };
    });

    this.setState({ config: update });
  },

  _toggleStatus(className) {
    return () => {
      const disabledProcessors = this.state.config.disabled_processors;
      const update = ObjectUtils.clone(this.state.config);
      const { checked } = this.inputs[className];

      if (checked) {
        update.disabled_processors = disabledProcessors.filter((p) => p !== className);
      } else if (disabledProcessors.indexOf(className) === -1) {
        update.disabled_processors.push(className);
      }

      this.setState({ config: update });
    };
  },

  _hasNoActiveProcessor() {
    return this.state.config.disabled_processors.length >= this.state.config.processor_order.length;
  },

  _noActiveProcessorWarning() {
    if (this._hasNoActiveProcessor()) {
      return (
        <Alert bsStyle="danger">
          <strong>ERROR:</strong> No active message processor!
        </Alert>
      );
    }
  },

  _summary() {
    return this.state.config.processor_order.map((processor, idx) => {
      const status = this.state.config.disabled_processors.filter((p) => p === processor.class_name).length > 0 ? 'disabled' : 'active';

      return (
        <tr key={idx}>
          <td>{idx + 1}</td>
          <td>{processor.name}</td>
          <td>{status}</td>
        </tr>
      );
    });
  },

  _sortableItems() {
    return this.state.config.processor_order.map((processor) => {
      return { id: processor.class_name, title: processor.name };
    });
  },

  _statusForm() {
    return ObjectUtils.clone(this.state.config.processor_order).sort((a, b) => naturalSort(a.name, b.name)).map((processor, idx) => {
      const enabled = this.state.config.disabled_processors.filter((p) => p === processor.class_name).length < 1;

      return (
        <tr key={idx}>
          <td>{processor.name}</td>
          <td>
            <input ref={(elem) => { this.inputs[processor.class_name] = elem; }}
                   type="checkbox"
                   checked={enabled}
                   onChange={this._toggleStatus(processor.class_name)} />
          </td>
        </tr>
      );
    });
  },

  render() {
    return (
      <div>
        <h2>Message Processors Configuration</h2>
        <p>The following message processors are executed in order. Disabled processors will be skipped.</p>

        <Table striped bordered condensed className="top-margin">
          <thead>
            <tr>
              <th>#</th>
              <th>Processor</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {this._summary()}
          </tbody>
        </Table>

        <IfPermitted permissions="clusterconfigentry:edit">
          <Button bsStyle="info" bsSize="xs" onClick={this._openModal}>Update</Button>
        </IfPermitted>

        <BootstrapModalForm ref={(configModal) => { this.configModal = configModal; }}
                            title="Update Message Processors Configuration"
                            onSubmitForm={this._saveConfig}
                            onModalClose={this._resetConfig}
                            submitButtonText="Save">
          <h3>Order</h3>
          <p>Use drag and drop to change the execution order of the message processors.</p>
          <SortableList items={this._sortableItems()} onMoveItem={this._updateSorting} />

          <h3>Status</h3>
          <p>Change the checkboxes to change the status of a message processor.</p>
          <Table striped bordered condensed className="top-margin">
            <thead>
              <tr>
                <th>Processor</th>
                <th>Enabled</th>
              </tr>
            </thead>
            <tbody>
              {this._statusForm()}
            </tbody>
          </Table>
          {this._noActiveProcessorWarning()}
        </BootstrapModalForm>
      </div>
    );
  },
});

export default MessageProcessorsConfig;
