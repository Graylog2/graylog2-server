import $ from 'jquery';

import PropTypes from 'prop-types';

import React from 'react';
import createReactClass from 'create-react-class';
import { Input } from 'components/bootstrap';

import BootstrapModalForm from '../bootstrap/BootstrapModalForm';

import CombinedProvider from 'injection/CombinedProvider';
const { DashboardsActions } = CombinedProvider.get('Dashboards');

const EditDashboardModal = createReactClass({
  displayName: 'EditDashboardModal',

  propTypes: {
    action: PropTypes.oneOf(['create', 'edit']),
    description: PropTypes.string,
    id: PropTypes.string,
    onSaved: PropTypes.func,
    title: PropTypes.string,
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
      <BootstrapModalForm ref={(modal) => { this.modal = modal; }}
                          title={this._isCreateModal() ? 'New Dashboard' : `Edit Dashboard ${this.props.title}`}
                          onSubmitForm={this._save}
                          submitButtonText="Save">
        <fieldset>
          <Input id={`${this.props.id}-title`} type="text" label="Title:" onChange={this._onTitleChange} value={this.state.title} autoFocus required />
          <Input id={`${this.props.id}-description`} type="text" label="Description:" name="Description" onChange={this._onDescriptionChange} value={this.state.description} required />
        </fieldset>
      </BootstrapModalForm>
    );
  },

  close() {
    this.modal.close();
  },

  open() {
    this.modal.open();
  },

  _save() {
    let promise;

    if (this._isCreateModal()) {
      promise = DashboardsActions.create(this.state.title, this.state.description);
      promise.then((id) => {
        this.close();

        if (typeof this.props.onSaved === 'function') {
          this.props.onSaved(id);
        }

        this.setState(this.getInitialState());
      });
    } else {
      promise = DashboardsActions.update(this.state);
      promise.then(() => {
        this.close();

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
