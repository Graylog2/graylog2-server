import React, { Component } from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';

import 'brace/mode/lucene';
import 'enterprise/components/searchbar/queryinput/ace-queryinput';
import 'brace/ext/language_tools';

import AceEditor from 'react-ace';

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

const _snippets = {
  getCompletions: _completions([]),
};

const _extractFields = (queryResult) => {
  if (!queryResult) {
    return null;
  }
  // TODO this requires that each query actually has a message list available.
  const searchTypes = queryResult.searchTypes;
  // TODO how do we deal with multiple lists? is that even useful?
  const messagesSearchType = _.find(searchTypes, t => t.type === 'messages');
  return messagesSearchType !== undefined ? messagesSearchType.fields : [];
};

class QueryInput extends Component {
  constructor(props) {
    super(props);
    this.state = {
      value: props.value,
    };
  }

  componentDidMount() {
    this.editor.editor.commands.addCommand({
      name: 'Execute',
      bindKey: { win: 'Enter', mac: 'Enter' },
      exec: this._onExecute,
    });

    this.editor.editor.completers.push(_snippets);
  }
  componentWillReceiveProps(nextProps) {
    const fields = _extractFields(nextProps.result);
    _snippets.getCompletions = _completions(fields);
  }

  editor = undefined;
  _bindEditor(editor) {
    if (editor) {
      this.editor = editor;
    }
  }

  _onChange = (newValue) => {
    this.setState({ value: newValue }, () => this.props.onChange(this.state.value));
  };

  _onExecute = () => {
    this.props.onChange(this.state.value);
    this.props.onExecute();
  };

  render() {
    return (
      <div className="query">
        <AceEditor mode="lucene"
                   ref={editor => this._bindEditor(editor)}
                   theme="ace-queryinput"
                   onChange={this._onChange}
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
                     selectionStyle: 'line',
                   }}
                   style={{
                     width: '100%',
                     marginTop: '10px',
                   }} />
      </div>
    );
  }
}

QueryInput.propTypes = {
  result: PropTypes.object,
  onChange: PropTypes.func.isRequired,
  onExecute: PropTypes.func.isRequired,
  value: PropTypes.string.isRequired,
};

QueryInput.defaultProps = {
  result: {},
};

export default QueryInput;
