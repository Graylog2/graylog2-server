import PropTypes from 'prop-types';
import React from 'react';
import { MenuItem } from 'react-bootstrap';

import { BootstrapModalForm, Input } from 'components/bootstrap';

import style from './CopyModal.css';

class CopyConfigurationModal extends React.Component {
  static propTypes = {
    configuration: PropTypes.object.isRequired,
    copyConfiguration: PropTypes.func.isRequired,
    validateConfiguration: PropTypes.func.isRequired,
  };

  static defaultProps = {
    id: '',
    name: '',
  };

  state = {
    id: this.props.configuration.id,
    name: '',
    error: false,
    error_message: '',
  };

  openModal = () => {
    this.refs.modal.open();
  };

  _getId = (prefixIdName) => {
    return `${prefixIdName}-${this.state.id}`;
  };

  _closeModal = () => {
    this.refs.modal.close();
  };

  _saved = () => {
    this._closeModal();
    this.setState({ name: '' });
  };

  _save = () => {
    const configuration = this.state;

    if (!configuration.error) {
      this.props.copyConfiguration(this.state.id, this.state.name, this._saved);
    }
  };

  _changeName = (event) => {
    const nextName = event.target.value;
    this.setState({ name: nextName });
    this.props.validateConfiguration({ name: nextName }).then((validation) => {
      let errorMessage = '';
      if (validation.errors.name) {
        errorMessage = validation.errors.name[0];
      }
      this.setState({ error: validation.failed, error_message: errorMessage });
    });
  };

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
                   id={this._getId('configuration-name')}
                   label="Name"
                   defaultValue={this.state.name}
                   onChange={this._changeName}
                   bsStyle={this.state.error ? 'error' : null}
                   help={this.state.error ? this.state.error_message : 'Type a name for the new configuration'}
                   autoFocus
                   required />
          </fieldset>
        </BootstrapModalForm>
      </span>
    );
  }
}

export default CopyConfigurationModal;
