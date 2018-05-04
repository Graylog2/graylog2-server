import PropTypes from 'prop-types';
import React from 'react';
import { BootstrapModalForm, Input } from 'components/bootstrap';

import { Button, Panel } from 'react-bootstrap';

class EditPatternModal extends React.Component {
  static propTypes = {
    id: PropTypes.string,
    name: PropTypes.string,
    pattern: PropTypes.string,
    create: PropTypes.bool,
    sampleData: PropTypes.string,
    savePattern: PropTypes.func.isRequired,
    testPattern: PropTypes.func,
    validPatternName: PropTypes.func.isRequired,
  };

  static defaultProps = {
    id: '',
    name: '',
    pattern: '',
    create: false,
    sampleData: '',
    testPattern: () => {},
  };

  state = {
    id: this.props.id,
    name: this.props.name,
    pattern: this.props.pattern,
    sampleData: this.props.sampleData,
    test_result: '',
    test_error: undefined,
    error: false,
    error_message: '',
  };

  openModal = () => {
    this.modal.open();
  };

  _onPatternChange = (event) => {
    this.setState({ pattern: event.target.value });
  };

  _onNameChange = (event) => {
    const name = event.target.value;

    if (!this.props.validPatternName(name)) {
      this.setState({ name: name, error: true, error_message: 'Pattern with that name already exists!' });
    } else {
      this.setState({ name: name, error: false, error_message: '' });
    }
  };

  _onSampleDataChange = (event) => {
    this.setState({ sampleData: event.target.value });
  };

  _getId = (prefixIdName) => {
    return this.state.name !== undefined ? prefixIdName + this.state.name : prefixIdName;
  };

  _closeModal = () => {
    this.modal.close();
  };

  _saved = () => {
    this._closeModal();
    if (this.props.create) {
      this.setState({ name: '', pattern: '', sampleData: '', test_result: '' });
    }
  };

  _save = () => {
    const pattern = this.state;

    if (!pattern.error) {
      this.props.savePattern(pattern, this._saved);
    }
  };

  _testPattern = () => {
    if (this.state.name === '' || this.state.pattern === '') {
      this.setState({ error: true, error_message: 'To test the pattern a name and a pattern must be given!' });
      return;
    }

    this.setState({ error: false, error_message: '' });

    this.props.testPattern(this.state, (response) => {
      this.setState({ test_result: JSON.stringify(response, null, 2), test_error: undefined });
    }, (errMessage) => {
      this.setState({ test_result: '', test_error: errMessage });
    });
  };

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
        <BootstrapModalForm ref={(modal) => { this.modal = modal; }}
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
                   help={this.state.error ? this.state.error_message : "Under this name the pattern will be stored and can be used like: '%{THISNAME}' later on "}
                   autoFocus
                   required />
            <Input type="textarea"
                   id={this._getId('pattern')}
                   label="Pattern"
                   help="The pattern which will match the log line e.g: '%{IP:client}' or '.*?'"
                   onChange={this._onPatternChange}
                   value={this.state.pattern}
                   required />
            { this.state.test_error &&
            <Panel bsStyle="danger" header="Grok Error">
              <code style={{ display: 'block', whiteSpace: 'pre-wrap' }} >{this.state.test_error}</code>
            </Panel>
            }
            <Input type="textarea"
                   id={this._getId('sampleData')}
                   label="Sample Data"
                   help="Here you can add sample data to test your pattern"
                   onChange={this._onSampleDataChange}
                   value={this.state.sampleData} />
            <Button bsStyle="info" onClick={this._testPattern}>Test with Sample Data</Button>
            <br />
            <br />
            <Input type="textarea"
                   id={this._getId('test_result')}
                   readOnly
                   rows={8}
                   help="Will contain the result of your test in a JSON format"
                   label="Test Result"
                   value={this.state.test_result} />
          </fieldset>
        </BootstrapModalForm>
      </span>
    );
  }
}

export default EditPatternModal;
