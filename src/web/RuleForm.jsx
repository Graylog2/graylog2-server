import React, { PropTypes } from 'react';

import { Row, Col } from 'react-bootstrap';
import { Input } from 'react-bootstrap';

import AceEditor from 'react-ace';
import brace from 'brace';

import 'brace/mode/text';
import 'brace/theme/chrome';

import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';

const RuleForm = React.createClass({
  propTypes: {
    rule: PropTypes.object,
    create: React.PropTypes.bool,
    save: React.PropTypes.func.isRequired,
    validateName: React.PropTypes.func.isRequired,
  },

  getDefaultProps() {
    return {
      rule: {
        id: '',
        title: '',
        description: '',
        source: '',
      }
    }
  },

  getInitialState() {
    const rule = this.props.rule;
    return {
      // when editing, take the rule that's been passed in
      rule: {
        id: rule.id,
        title: rule.title,
        description: rule.description,
        source: rule.source
      },
      error: false,
      error_message: '',
    }
  },

  openModal() {
    this.refs.modal.open();
  },

  _onSourceChange(value) {
    const rule = this.state.rule;
    rule.source = value;
    this.setState({rule: rule});
  },

  _onDescriptionChange(event) {
    const rule = this.state.rule;
    rule.description = event.target.value;
    this.setState({rule: rule});
  },

  _onTitleChange(event) {
    const title = event.target.value;

    const rule = this.state.rule;
    rule.title = title;

    if (!this.props.validateName(title)) {
      this.setState({rule: rule, error: true, error_message: 'A rule with that title already exists!'});
    } else {
      this.setState({rule: rule, error: false, error_message: ''});
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
      this.setState({rule: {}});
    }
  },
  _save() {
    if (!this.state.error) {
      this.props.save(this.state.rule, this._saved);
    }
  },
  render() {
    let triggerButtonContent;
    if (this.props.create) {
      triggerButtonContent = 'Create rule';
    } else {
      triggerButtonContent = <span>Edit</span>;
    }
    return (
      <span>
        <button onClick={this.openModal}
                className={this.props.create ? 'btn btn-success' : 'btn btn-info btn-xs'}>
          {triggerButtonContent}
        </button>
        <BootstrapModalForm ref="modal"
                            title={`${this.props.create ? 'Create' : 'Edit'} Processing Rule ${this.state.rule.title}`}
                            onSubmitForm={this._save}
                            submitButtonText="Save">
          <fieldset>
            <Input type="text"
                   id={this._getId('title')}
                   label="Name"
                   onChange={this._onTitleChange}
                   value={this.state.rule.title}
                   bsStyle={this.state.error ? 'error' : null}
                   help={this.state.error ? this.state.error_message : null}
                   autoFocus
                   required/>
            <Input type="textarea"
                   id={this._getId('description')}
                   label="Description (optional)"
                   onChange={this._onDescriptionChange}
                   value={this.state.rule.description}/>

            <label>Rule source</label>
            <div style={{border: "1px solid lightgray", borderRadius: 5}}>
              <AceEditor
                mode="text"
                theme="chrome"
                name={"source" + (this.props.create ? "-create" : "-edit")}
                fontSize={11}
                height="14em"
                width="100%"
                value={this.state.rule.source}
                onChange={this._onSourceChange}
              />
            </div>
          </fieldset>
        </BootstrapModalForm>
      </span>
    );
  },
});

export default RuleForm;