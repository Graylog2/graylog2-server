import React from 'react';
import { PropTypes } from 'prop-types';
import { Resizable } from 'react-resizable';
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
    /** Editor height in pixels. */
    height: PropTypes.number,
    /** Specifies a unique ID for the source code editor. */
    id: PropTypes.string.isRequired,
    /** Function called on editor load. The first argument is the instance of the editor. */
    onLoad: PropTypes.func,
    /** Function called when the value of the text changes. It receives the the new value and an event as arguments. */
    onChange: PropTypes.func,
    /** Specifies if the editor should be in read-only mode. */
    readOnly: PropTypes.bool,
    /** Specifies if the editor should be resizable by the user. */
    resizable: PropTypes.bool,
    /** Specifies the theme to use for the editor. */
    theme: PropTypes.oneOf(['light', 'dark']),
    /** Text to use in the editor. */
    value: PropTypes.string,
    /** Editor width in pixels. Use `Infinity` to indicate the editor should use 100% of its container's width. */
    width: PropTypes.number,
  }

  static defaultProps = {
    focus: false,
    fontSize: 13,
    height: 200,
    onChange: () => {},
    onLoad: () => {},
    readOnly: false,
    resizable: true,
    theme: 'light',
    value: '',
    width: Infinity,
  }

  constructor(props) {
    super(props);
    this.state = {
      height: props.height,
      width: props.width,
      selectedText: '',
    };
  }

  componentDidUpdate(prevProps) {
    if (this.props.height !== prevProps.height || this.props.width !== prevProps.width) {
      this.reloadEditor();
    }
  }

  handleResize = (event, { size }) => {
    const { height, width } = size;
    this.setState({ height: height, width: width }, this.reloadEditor);
  }

  reloadEditor = () => {
    this.reactAce.editor.resize();
  }

  resetUndoHistory = () => {
    // Hack to not clear editor form when executing undo action.
    // See https://github.com/Graylog2/graylog-plugin-pipeline-processor/issues/224
    if (!this.clearedUndoHistory) {
      try {
        this.reactAce.editor.getSession().getUndoManager().reset();
        this.clearedUndoHistory = true;
      } catch (e) {
        // Do nothing
      }
    }
  }

  render() {
    const { height, width } = this.state;
    const { theme, resizable } = this.props;
    const containerStyle = `${style.sourceCodeEditor} ${theme !== 'light' && style.darkMode} ${!resizable && style.static}`;
    return (
      <Resizable height={height}
                 width={width}
                 minConstraints={[200, 200]}
                 onResize={this.handleResize}>
        <div className={containerStyle} style={{ height: height, width: Number.isNaN(width) ? '100%' : width }}>
          <AceEditor ref={(c) => { this.reactAce = c; }}
                     editorProps={{ $blockScrolling: 'Infinity' }}
                     focus={this.props.focus}
                     fontSize={this.props.fontSize}
                     mode="text"
                     theme={this.props.theme === 'light' ? 'tomorrow' : 'monokai'}
                     name={this.props.id}
                     height="100%"
                     onInput={this.resetUndoHistory}
                     onLoad={this.props.onLoad}
                     onChange={this.props.onChange}
                     readOnly={this.props.readOnly}
                     defaultValue={this.props.value}
                     value={this.props.value}
                     width="100%" />
        </div>
      </Resizable>
    );
  }
}

export default SourceCodeEditor;
