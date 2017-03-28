import $ from 'jquery';

import React from 'react';
import { Input } from 'components/bootstrap';

import BootstrapModalForm from '../bootstrap/BootstrapModalForm';

import StoreProvider from 'injection/StoreProvider';
const DashboardsStore = StoreProvider.getStore('Dashboards');

const EditDashboardModal = React.createClass({
  propTypes: {
    action: React.PropTypes.oneOf(['create', 'edit']),
    description: React.PropTypes.string,
    id: React.PropTypes.string,
    onSaved: React.PropTypes.func,
    title: React.PropTypes.string,
  },
  getInitialState() {
    return {
      id: this.props.id,
      description: this.props.description,
      title: this.props.title,
    };
  },
  getDefaultProps() {
    return {
      action: 'create',
    };
  },
  render() {
    return (
      <BootstrapModalForm ref="modal"
                          title={this._isCreateModal() ? 'New Dashboard' : `Edit Dashboard ${this.props.title}`}
                          onSubmitForm={this._save}
                          submitButtonText="Save">
        <fieldset>
          <Input id={`${this.props.id}-title`} type="text" label="Title:" onChange={this._onTitleChange} value={this.state.title} autoFocus required />
          <Input type="text" label="Description:" name="Description" onChange={this._onDescriptionChange} value={this.state.description} required />
        </fieldset>
      </BootstrapModalForm>
    );
  },
  close() {
    this.refs.modal.close();
  },
  open() {
    this.refs.modal.open();
  },
  _save() {
    let promise;

    if (this._isCreateModal()) {
      promise = DashboardsStore.createDashboard(this.state.title, this.state.description);
      promise.then((id) => {
        this.close();

        if (typeof this.props.onSaved === 'function') {
          this.props.onSaved(id);
        }

        this.setState(this.getInitialState());
      });
    } else {
      promise = DashboardsStore.saveDashboard(this.state);
      promise.then(() => {
        this.close();

        const idSelector = `[data-dashboard-id="${this.state.id}"]`;
        const $title = $(`${idSelector}.dashboard-title`);
        if ($title.length > 0) {
          $title.html(this.state.title);
        }

        const $description = $(`${idSelector}.dashboard-description`);
        if ($description.length > 0) {
          $description.html(this.state.description);
        }

        if (typeof this.props.onSaved === 'function') {
          this.props.onSaved(this.state.id);
        }
      });
    }
  },
  _onDescriptionChange(event) {
    this.setState({ description: event.target.value });
  },
  _onTitleChange(event) {
    this.setState({ title: event.target.value });
  },
  _isCreateModal() {
    return this.props.action === 'create';
  },
});

export default EditDashboardModal;
