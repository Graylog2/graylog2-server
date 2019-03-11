// @flow strict
import React, { Component } from 'react';
import PropTypes from 'prop-types';

import AceEditor from './queryinput/ace';
import SearchBarAutoCompletions from './SearchBarAutocompletions';
import FieldNameCompletion from './completions/FieldNameCompletion';
import OperatorCompletion from './completions/OperatorCompletion';
import type { AutoCompleter, Editor } from './ace-types';
import ParameterCompletion from './completions/ParameterCompletion';

const _placeholderNode = (placeholder) => {
  const node = document.createElement('div');
  node.textContent = placeholder;
  node.className = 'ace_invisible ace_emptyMessage';
  node.style.padding = '0 9px';
  node.style.color = '#aaa';
  return node;
};

type Props = {
  value: string,
  // eslint-disable-next-line no-undef
  completerClass?: Class<AutoCompleter>,
  onBlur?: (string) => void,
  onChange: (string) => Promise<string>,
  onExecute: (string) => void,
  placeholder: string,
};

type State = {
  value: string,
};

class QueryInput extends Component<Props, State> {
  static defaultProps = {
    onBlur: () => {},
    completerClass: SearchBarAutoCompletions,
    value: '',
    placeholder: '',
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      value: props.value,
    };
    const CompleterClass = props.completerClass;
    // $FlowFixMe: tailored for this specific one for now
    this.completer = new CompleterClass([
      new FieldNameCompletion(),
      new OperatorCompletion(),
      new ParameterCompletion(),
    ]);
  }

  componentDidMount() {
    const editor = this.editor && this.editor.editor;
    if (editor) {
      editor.commands.addCommand({
        name: 'Execute',
        bindKey: { win: 'Enter', mac: 'Enter' },
        exec: this._onExecute,
      });

      editor.setFontSize(16);

      editor.completers = [this.completer];

      if (!this.props.value && !this._placeholderExists(editor)) {
        this._addPlaceholder(editor);
      }
    }
  }

  componentWillReceiveProps(nextProps: Props) {
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

  completer: AutoCompleter;
  isFocussed: boolean;
  editor: {
    editor: Editor,
  } | typeof undefined;

  _addPlaceholder = (editor: Editor) => {
    if (!editor.renderer.emptyMessageNode) {
      const node = _placeholderNode(this.props.placeholder);
      // eslint-disable-next-line no-param-reassign
      editor.renderer.emptyMessageNode = node;
      editor.renderer.scroller.appendChild(node);
    }
  };

  _removePlaceholder = (editor: Editor) => {
    if (editor.renderer.emptyMessageNode) {
      editor.renderer.scroller.removeChild(editor.renderer.emptyMessageNode);
      // eslint-disable-next-line no-param-reassign
      editor.renderer.emptyMessageNode = null;
    }
  };

  _placeholderExists = (editor: Editor) => {
    const { emptyMessageNode } = editor.renderer;
    return emptyMessageNode !== undefined && emptyMessageNode !== null;
  };

  editor = undefined;

  _bindEditor(editor: { editor: Editor }) {
    if (editor) {
      this.editor = editor;
    }
  }

  _onChange = (newValue: string) => {
    this.setState({ value: newValue });
  };

  _onBlur = () => {
    this.isFocussed = false;
    const editor = this.editor && this.editor.editor;
    if (editor) {
      const shouldShow = !editor.session.getValue().length;
      if (shouldShow && !this._placeholderExists(editor)) {
        this._addPlaceholder(editor);
      }
    }
    this.props.onChange(this.state.value).then(this.props.onBlur);
  };

  _onFocus = () => {
    this.isFocussed = true;
    const editor = this.editor && this.editor.editor;
    if (editor && this._placeholderExists(editor)) {
      this._removePlaceholder(editor);
    }
  };

  _onExecute = (editor: Editor) => {
    const { onChange, onExecute } = this.props;
    if (editor.completer && editor.completer.popup) {
      editor.completer.popup.hide();
    }
    onChange(this.state.value).then(onExecute);
  };

  render() {
    const { onBlur, onChange, onExecute, placeholder, value, ...rest } = this.props;
    return (
      <div className="query" style={{ display: 'flex' }}>
        <AceEditor mode="lucene"
                   ref={editor => editor && this._bindEditor(editor)}
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
  onBlur: PropTypes.func,
  onChange: PropTypes.func.isRequired,
  onExecute: PropTypes.func.isRequired,
  placeholder: PropTypes.string,
  value: PropTypes.string,
};

export default QueryInput;
