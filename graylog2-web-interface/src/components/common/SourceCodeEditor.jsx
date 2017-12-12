import React from 'react';
import { PropTypes } from 'prop-types';
import 'brace';
import AceEditor from 'react-ace';

import 'brace/mode/text';
import 'brace/theme/tomorrow';
import 'brace/theme/monokai';
import style from './SourceCodeEditor.css';

/**
 * Component that renders a source code editor input. This is what powers the pipeline rules and collector
 * editors.
 */
class SourceCodeEditor extends React.Component {
  static propTypes = {
    /** Specifies if the source code editor should have the input focus or not. */
    focus: PropTypes.bool,
    /** Specifies the font size in pixels to use in the text editor. */
    fontSize: PropTypes.number,
    /** Specifies a unique ID for the source code editor. */
    id: PropTypes.string.isRequired,
    /** Function called on editor load. The first argument is the instance of the editor. */
    onLoad: PropTypes.func,
    /**
     * Function called when the value of the text changes. It receives the
     * the new value and an event as arguments.
     */
    onChange: PropTypes.func.isRequired,
    /** Specifies the theme to use for the editor. */
    theme: PropTypes.oneOf(['light', 'dark']),
    /** Text to use in the editor. */
    value: PropTypes.string,
  };

  static defaultProps = {
    focus: false,
    fontSize: 13,
    onLoad: () => {},
    theme: 'light',
    value: '',
  };

  render() {
    return (
      <div className={style.sourceCodeEditor}>
        <AceEditor editorProps={{ $blockScrolling: 'Infinity' }}
                   focus={this.props.focus}
                   fontSize={this.props.fontSize}
                   mode="text"
                   theme={this.props.theme === 'light' ? 'tomorrow' : 'monokai'}
                   name={this.props.id}
                   height="18em"
                   onLoad={this.props.onLoad}
                   onChange={this.props.onChange}
                   value={this.props.value}
                   width="100%" />
      </div>
    );
  }
}

export default SourceCodeEditor;
