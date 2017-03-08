import React from 'react';
import { Button, Alert, Table } from 'react-bootstrap';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import { IfPermitted, SortableList } from 'components/common';
import ObjectUtils from 'util/ObjectUtils';
import naturalSort from 'javascript-natural-sort';

const MessageProcessorsConfig = React.createClass({
  propTypes: {
    config: React.PropTypes.object.isRequired,
    updateConfig: React.PropTypes.func.isRequired,
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

  _openModal() {
    this.refs.configModal.open();
  },

  _closeModal() {
    this.refs.configModal.close();
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
      const checked = this.refs[className].checked;

      if (checked) {
        update.disabled_processors = disabledProcessors.filter(p => p !== className);
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
      const status = this.state.config.disabled_processors.filter(p => p === processor.class_name).length > 0 ? 'disabled' : 'active';
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
      const enabled = this.state.config.disabled_processors.filter(p => p === processor.class_name).length < 1;

      return (
        <tr key={idx}>
          <td>{processor.name}</td>
          <td>
            <input ref={processor.class_name}
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

        <BootstrapModalForm ref="configModal"
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
