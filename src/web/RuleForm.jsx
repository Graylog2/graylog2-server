import React, { PropTypes } from 'react';

import { Row, Col } from 'react-bootstrap';
import { Input } from 'react-bootstrap';

import AceEditor from 'react-ace';
import brace from 'brace';

import 'brace/mode/text';
import 'brace/theme/chrome';

import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';

const RuleForm = React.createClass({
  parseTimer: undefined,
  propTypes: {
    rule: PropTypes.object,
    create: React.PropTypes.bool,
    save: React.PropTypes.func.isRequired,
    validateRule: React.PropTypes.func.isRequired,
  },

  getDefaultProps() {
    return {
      rule: {
        id: '',
        title: '',
        description: '',
        source: '',
      },
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
      editor: undefined,
      parseErrors: [],
    }
  },

  componentWillUnmount() {
    if (this.parseTimer !== undefined) {
      clearTimeout(this.parseTimer);
      this.parseTimer = undefined;
    }
  },

  openModal() {
    this.refs.modal.open();
  },

  _updateEditor() {
    const session = this.state.editor.session;
    const annotations = this.state.parseErrors.map(e => {
      return {row: e.line - 1, column: e.position_in_line - 1, text: e.reason, type: "error"}
    });
    session.setAnnotations(annotations);
  },

  _setParseErrors(errors) {
    this.setState({parseErrors: errors}, this._updateEditor);
  },

  _onSourceChange(value) {
    // don't try to parse the previous value, gets reset below
    if (this.parseTimer !== undefined) {
      clearTimeout(this.parseTimer);
    }
    const rule = this.state.rule;
    rule.source = value;
    this.setState({rule: rule});

    if (this.props.validateRule) {
      // have the caller validate the rule after typing stopped for a while. usually this will mean send to server to parse
      this.parseTimer = setTimeout(() => this.props.validateRule(rule, this._setParseErrors), 500);
    }
  },

  _onDescriptionChange(event) {
    const rule = this.state.rule;
    rule.description = event.target.value;
    this.setState({rule: rule});
  },

  _onTitleChange(event) {
    const rule = this.state.rule;
    rule.title = event.target.value;
    this.setState({rule: rule});
  },

  _onLoad(editor) {
    this.setState({editor: editor});
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
    if (this.state.parseErrors.length === 0) {
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
                editorProps={{ $blockScrolling: "Infinity"}}
                value={this.state.rule.source}
                onLoad={this._onLoad}
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