import React, { Component } from 'react';
import PropTypes from 'prop-types';
import Immutable from 'immutable';
import _ from 'lodash';

import 'brace/mode/lucene';
import 'enterprise/components/searchbar/queryinput/ace-queryinput';
import 'brace/ext/language_tools';

import AceEditor from 'react-ace';
import SearchBarAutoCompletions from './SearchBarAutocompletions';

const _completions = (fields) => {
  return (editor, session, pos, prefix, callback) => {
    const results = fields.keySeq().filter(field => field.indexOf(prefix) >= 0)
      .map((field) => {
        return {
          name: field,
          value: field,
          score: 1,
          meta: 'field',
        };
      }).toJS();
    callback(null, results);
  };
};

class QueryInput extends Component {
  constructor(props) {
    super(props);
    this.state = {
      value: props.value,
    };
    this.completer = new SearchBarAutoCompletions();
  }

  componentDidMount() {
    this.editor.editor.commands.addCommand({
      name: 'Execute',
      bindKey: { win: 'Enter', mac: 'Enter' },
      exec: this._onExecute,
    });

    this.editor.editor.setFontSize(16);

    this.editor.editor.completers.push(this.completer);
  }
  componentWillReceiveProps(nextProps) {
    if (nextProps.value !== this.state.value) {
      this.setState({ value: nextProps.value });
    }
    if (this.editor) {
      const { editor } = this.editor;
      if (nextProps.value && this._placeholderExists(editor)) {
        this._removePlaceholder(this.editor.editor);
      }

      if (!nextProps.value && !this.isFocussed && !this._placeholderExists(editor)) {
        this._addPlaceholder(editor);
      }
    }
  }

  _placeholderNode(placeholder) {
    const node = document.createElement('div');
    node.textContent = placeholder;
    node.className = 'ace_invisible ace_emptyMessage';
    node.style.padding = '0 9px';
    node.style.color = '#aaa';
    return node;
  }

  _addPlaceholder = (editor) => {
    if (!editor.renderer.emptyMessageNode) {
      const node = this._placeholderNode(this.props.placeholder);
      editor.renderer.emptyMessageNode = node;
      editor.renderer.scroller.appendChild(node);
    }
  };

  _removePlaceholder = (editor) => {
    if (editor.renderer.emptyMessageNode) {
      editor.renderer.scroller.removeChild(editor.renderer.emptyMessageNode);
      editor.renderer.emptyMessageNode = null;
    }
  };

  _placeholderExists = (editor) => {
    const { emptyMessageNode } = editor.renderer;
    return emptyMessageNode !== undefined && emptyMessageNode !== null;
  };

  editor = undefined;

  _bindEditor(editor) {
    if (editor) {
      this.editor = editor;
    }
  }

  _onChange = (newValue) => {
    this.setState({ value: newValue }, () => this.props.onChange(this.state.value));
  };

  _onBlur = () => {
    this.isFocussed = false;
    const editor = this.editor.editor;
    const shouldShow = !editor.session.getValue().length;
    if (shouldShow && !this._placeholderExists(editor)) {
      this._addPlaceholder(editor);
    }
  };

  _onFocus = () => {
    this.isFocussed = true;
    const editor = this.editor.editor;
    if (this._placeholderExists(editor)) {
      this._removePlaceholder(editor);
    }
  };

  _onExecute = () => {
    this.props.onChange(this.state.value);
    this.props.onExecute();
  };

  render() {
    return (
      <div className="query" style={{ display: 'flex' }}>
        <AceEditor mode="lucene"
                   ref={editor => this._bindEditor(editor)}
                   theme="ace-queryinput"
                   onBlur={this._onBlur}
                   onChange={this._onChange}
                   onFocus={this._onFocus}
                   value={this.state.value}
                   name="QueryEditor"
                   showGutter={false}
                   showPrintMargin={false}
                   highlightActiveLine={false}
                   minLines={1}
                   maxLines={1}
                   enableBasicAutocompletion
                   enableLiveAutocompletion
                   editorProps={{
                     $blockScrolling: Infinity,
                     selectionStyle: 'line',
                   }}
                   fontSize={13}
                   style={{
                     marginTop: '9px',
                     height: '34px',
                     width: '100%'
                   }} />
      </div>
    );
  }
}

QueryInput.propTypes = {
  onChange: PropTypes.func.isRequired,
  onExecute: PropTypes.func.isRequired,
  placeholder: PropTypes.string,
  result: PropTypes.object,
  value: PropTypes.string,
};

QueryInput.defaultProps = {
  result: undefined,
  value: '',
  placeholder: '',
};

export default QueryInput;
