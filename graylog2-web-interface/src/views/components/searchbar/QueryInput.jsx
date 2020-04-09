// @flow strict
import withPluginEntities from 'views/logic/withPluginEntities';
import PropTypes from 'prop-types';
import React, { Component } from 'react';
import type { AutoCompleter, Editor } from './ace-types';

import AceEditor from './queryinput/ace';
import type { Completer } from './SearchBarAutocompletions';
import SearchBarAutoCompletions from './SearchBarAutocompletions';

type Props = {|
  disabled: boolean,
  value: string,
  completers: Array<Completer>,
  completerClass?: Class<AutoCompleter>,
  onBlur?: (string) => void,
  onChange: (string) => Promise<string>,
  onExecute: (string) => void,
  placeholder: string,
|};

type State = {
};

class QueryInput extends Component<Props, State> {
  completer: AutoCompleter;

  editor: {
    editor: Editor,
  } | typeof undefined;

  static defaultProps = {
    disabled: false,
    onBlur: () => {},
    completerClass: SearchBarAutoCompletions,
    value: '',
    placeholder: '',
  };

  constructor(props: Props) {
    super(props);
    this.editor = undefined;
    const CompleterClass = props.completerClass;
    const { completers = [] } = props;
    // $FlowFixMe: tailored for this specific one for now
    this.completer = new CompleterClass(completers);
  }

  componentDidMount() {
    const editor = this.editor && this.editor.editor;
    if (editor) {
      editor.commands.addCommand({
        name: 'Execute',
        bindKey: { win: 'Enter', mac: 'Enter' },
        exec: this._onExecute,
      });

      editor.commands.removeCommands(['indent', 'outdent']);

      editor.setFontSize(16);

      editor.completers = [this.completer];
    }
  }

  _onExecute = (editor: Editor) => {
    const { onExecute, value } = this.props;
    if (editor.completer && editor.completer.popup) {
      editor.completer.popup.hide();
    }
    onExecute(value);
  };

  _bindEditor(editor: { editor: Editor }) {
    if (editor) {
      this.editor = editor;
    }
  }

  render() {
    const { disabled, onBlur, onChange, onExecute, placeholder, value } = this.props;
    return (
      <div className="query" style={{ display: 'flex' }} data-testid="query-input">
        <AceEditor mode="lucene"
                   disabled={disabled}
                   ref={(editor) => editor && this._bindEditor(editor)}
                   readOnly={disabled}
                   theme="ace-queryinput"
                   onBlur={onBlur}
                   onChange={onChange}
                   value={value}
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
                   placeholder={placeholder} />
      </div>
    );
  }
}

QueryInput.propTypes = {
  completers: PropTypes.array.isRequired,
  completerClass: PropTypes.any,
  disabled: PropTypes.bool,
  onBlur: PropTypes.func,
  onChange: PropTypes.func.isRequired,
  onExecute: PropTypes.func.isRequired,
  placeholder: PropTypes.string,
  value: PropTypes.string,
};

export default withPluginEntities(QueryInput, { completers: 'views.completers' });
