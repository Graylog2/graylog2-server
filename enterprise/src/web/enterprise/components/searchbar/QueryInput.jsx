import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { debounce } from 'lodash';

import 'brace';
import 'brace/mode/lucene';
import 'enterprise/components/searchbar/queryinput/ace-queryinput';
import 'brace/ext/language_tools';

import AceEditor from 'react-ace';
import SearchBarAutoCompletions from './SearchBarAutocompletions';

const _placeholderNode = (placeholder) => {
  const node = document.createElement('div');
  node.textContent = placeholder;
  node.className = 'ace_invisible ace_emptyMessage';
  node.style.padding = '0 9px';
  node.style.color = '#aaa';
  return node;
};

class QueryInput extends Component {
  constructor(props) {
    super(props);
    this.state = {
      value: props.value,
    };
    const CompleterClass = props.completerClass;
    this.completer = new CompleterClass();
  }

  componentDidMount() {
    const { editor } = this.editor;
    if (editor) {
      editor.commands.addCommand({
        name: 'Execute',
        bindKey: { win: 'Enter', mac: 'Enter' },
        exec: this._onExecute,
      });

      editor.setFontSize(16);

      editor.completers.push(this.completer);

      if (!this.props.value && !this._placeholderExists(editor)) {
        this._addPlaceholder(editor);
      }
    }
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

  _addPlaceholder = (editor) => {
    if (!editor.renderer.emptyMessageNode) {
      const node = _placeholderNode(this.props.placeholder);
      // eslint-disable-next-line no-param-reassign
      editor.renderer.emptyMessageNode = node;
      editor.renderer.scroller.appendChild(node);
    }
  };

  _removePlaceholder = (editor) => {
    if (editor.renderer.emptyMessageNode) {
      editor.renderer.scroller.removeChild(editor.renderer.emptyMessageNode);
      // eslint-disable-next-line no-param-reassign
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

  _debouncedOnChange = debounce(value => this.props.onChange(value), 700);

  _onChange = (newValue) => {
    this.setState({ value: newValue });
  };

  _onBlur = () => {
    this.isFocussed = false;
    const editor = this.editor.editor;
    if (editor) {
      const shouldShow = !editor.session.getValue().length;
      if (shouldShow && !this._placeholderExists(editor)) {
        this._addPlaceholder(editor);
      }
    }
    this.props.onChange(this.state.value).then(this.props.onBlur)
  };

  _onFocus = () => {
    this.isFocussed = true;
    const editor = this.editor.editor;
    if (editor && this._placeholderExists(editor)) {
      this._removePlaceholder(editor);
    }
  };

  _onExecute = () => {
    this.props.onChange(this.state.value).then(this.props.onExecute);
  };

  render() {
    const { onBlur, onChange, onExecute, placeholder, value, ...rest } = this.props;
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
                     width: '100%',
                   }}
                   {...rest}
        />
      </div>
    );
  }
}

QueryInput.propTypes = {
  completerClass: PropTypes.any,
  onChange: PropTypes.func.isRequired,
  onExecute: PropTypes.func.isRequired,
  placeholder: PropTypes.string,
  value: PropTypes.string,
};

QueryInput.defaultProps = {
  completerClass: SearchBarAutoCompletions,
  value: '',
  placeholder: '',
};

export default QueryInput;
