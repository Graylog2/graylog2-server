import React from 'react';
import { BootstrapModalForm, Input } from 'components/bootstrap';

const EditPatternModal = React.createClass({
  propTypes: {
    id: React.PropTypes.string,
    name: React.PropTypes.string,
    pattern: React.PropTypes.string,
    create: React.PropTypes.bool,
    savePattern: React.PropTypes.func.isRequired,
    validPatternName: React.PropTypes.func.isRequired,
  },
  getInitialState() {
    return {
      id: this.props.id,
      name: this.props.name,
      pattern: this.props.pattern,
      error: false,
      error_message: '',
    };
  },
  openModal() {
    this.refs.modal.open();
  },
  _onPatternChange(event) {
    this.setState({ pattern: event.target.value });
  },
  _onNameChange(event) {
    const name = event.target.value;

    if (!this.props.validPatternName(name)) {
      this.setState({ name: name, error: true, error_message: 'Pattern with that name already exists!' });
    } else {
      this.setState({ name: name, error: false, error_message: '' });
    }
  },
  _getId(prefixIdName) {
    return this.state.name !== undefined ? prefixIdName + this.state.name : prefixIdName;
  },
  _closeModal() {
    this.refs.modal.close();
  },
  _saved() {
    this._closeModal();
    if (this.props.create) {
      this.setState({ name: '', pattern: '' });
    }
  },
  _save() {
    const pattern = this.state;

    if (!pattern.error) {
      this.props.savePattern(pattern, this._saved);
    }
  },
  render() {
    let triggerButtonContent;
    if (this.props.create) {
      triggerButtonContent = 'Create pattern';
    } else {
      triggerButtonContent = <span>Edit</span>;
    }
    return (
      <span>
        <button onClick={this.openModal} className={this.props.create ? 'btn btn-success' : 'btn btn-info btn-xs'}>
          {triggerButtonContent}
        </button>
        <BootstrapModalForm ref="modal"
                                    title={`${this.props.create ? 'Create' : 'Edit'} Grok Pattern ${this.state.name}`}
                                    onSubmitForm={this._save}
                                    submitButtonText="Save">
          <fieldset>
            <Input type="text"
                           id={this._getId('pattern-name')}
                           label="Name"
                           onChange={this._onNameChange}
                           value={this.state.name}
                           bsStyle={this.state.error ? 'error' : null}
                           help={this.state.error ? this.state.error_message : null}
                           autoFocus
                           required />
            <Input type="textarea"
                           id={this._getId('pattern')}
                           label="Pattern"
                           onChange={this._onPatternChange}
                           value={this.state.pattern}
                           required />
          </fieldset>
        </BootstrapModalForm>
      </span>
    );
  },
});

export default EditPatternModal;
