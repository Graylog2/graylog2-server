import PropTypes from 'prop-types';
import React from 'react';
import { MenuItem } from 'react-bootstrap';

import { BootstrapModalForm, Input } from 'components/bootstrap';

import style from './CopyModal.css';

const CopyCollectorModal = React.createClass({
  propTypes: {
    id: PropTypes.string,
    copyCollector: PropTypes.func.isRequired,
    validateCollector: PropTypes.func.isRequired,
  },

  getDefaultProps() {
    return {
      id: '',
      name: '',
    };
  },

  getInitialState() {
    return {
      id: this.props.id,
      name: '',
      error: false,
      error_message: '',
    };
  },

  openModal() {
    this.refs.modal.open();
  },

  _getId(prefixIdName) {
    return `${prefixIdName}-${this.props.id}`;
  },

  _closeModal() {
    this.refs.modal.close();
  },

  _saved() {
    this._closeModal();
    this.setState({ name: '' });
  },

  _save() {
    const collector = this.state;

    if (!collector.error) {
      this.props.copyCollector(this.props.id, this.state.name, this._saved);
    }
  },

  _changeName(event) {
    const nextName = event.target.value;
    this.setState({ name: nextName });
    this.props.validateCollector(nextName).then(validation => (
      this.setState({ error: validation.error, error_message: validation.error_message })
    ));
  },

  render() {
    return (
      <span>
        <MenuItem className={style.menuItem} onSelect={this.openModal}>Clone</MenuItem>
        <BootstrapModalForm ref="modal"
                            title="Clone"
                            onSubmitForm={this._save}
                            submitButtonDisabled={this.state.error}
                            submitButtonText="Done">
          <fieldset>
            <Input type="text"
                   id={this._getId('collector-name')}
                   label="Name"
                   defaultValue={this.state.name}
                   onChange={this._changeName}
                   bsStyle={this.state.error ? 'error' : null}
                   help={this.state.error ? this.state.error_message : 'Type a name for the new collector'}
                   autoFocus
                   required />
          </fieldset>
        </BootstrapModalForm>
      </span>
    );
  },
});

export default CopyCollectorModal;
