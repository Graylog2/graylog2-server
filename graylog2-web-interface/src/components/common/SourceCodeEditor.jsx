import React from 'react';
import { PropTypes } from 'prop-types';
import { Resizable } from 'react-resizable';
import 'brace';
import AceEditor from 'react-ace';
import { Button, ButtonGroup, ButtonToolbar, OverlayTrigger, Tooltip } from 'react-bootstrap';

import { ClipboardButton } from 'components/common';

import 'brace/mode/json';
import 'brace/mode/lua';
import 'brace/mode/markdown';
import 'brace/mode/text';
import 'brace/mode/yaml';
import 'brace/theme/tomorrow';
import 'brace/theme/monokai';
import style from './SourceCodeEditor.css';

/**
 * Component that renders a source code editor input. This is what powers the pipeline rules and collector
 * editors.
 *
 * **Note:** The component needs to be used in a [controlled way](https://reactjs.org/docs/forms.html#controlled-components).
 * Letting the component handle its own internal state may lead to weird errors while typing.
 */
class SourceCodeEditor extends React.Component {
  static propTypes = {
    /**
     * Annotations to show in the editor's gutter. The format should be:
     * `[{ row: 0, column: 2, type: 'error', text: 'Some error.'}]`
     * The type value must be one of `error`, `warning`, or `info`.
     */
    annotations: PropTypes.array,
    /** Specifies if the source code editor should have the input focus or not. */
    focus: PropTypes.bool,
    /** Specifies the font size in pixels to use in the text editor. */
    fontSize: PropTypes.number,
    /** Editor height in pixels. */
    height: PropTypes.number,
    /** Specifies a unique ID for the source code editor. */
    id: PropTypes.string.isRequired,
    /** Specifies the mode to use in the editor. This is used for highlighting and auto-completion. */
    mode: PropTypes.oneOf(['json', 'lua', 'markdown', 'text', 'yaml']),
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
    /** Specifies if the editor should also include a toolbar. */
    toolbar: PropTypes.bool,
    /** Text to use in the editor. */
    value: PropTypes.string,
    /** Editor width in pixels. Use `Infinity` to indicate the editor should use 100% of its container's width. */
    width: PropTypes.number,
  }

  static defaultProps = {
    annotations: [],
    focus: false,
    fontSize: 13,
    height: 200,
    mode: 'text',
    onChange: () => {},
    onLoad: () => {},
    readOnly: false,
    resizable: true,
    theme: 'light',
    toolbar: true,
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
    if (this.props.resizable) {
      this.reactAce.editor.resize();
    }
  }

  isCopyDisabled = () => this.props.readOnly || this.state.selectedText === '';

  isPasteDisabled = () => this.props.readOnly;

  isRedoDisabled = () => this.props.readOnly || !this.reactAce || !this.reactAce.editor.getSession().getUndoManager().hasRedo();

  isUndoDisabled = () => this.props.readOnly || !this.reactAce || !this.reactAce.editor.getSession().getUndoManager().hasUndo();

  handleRedo = () => {
    this.reactAce.editor.redo();
    this.focusEditor();
  }

  handleUndo = () => {
    this.reactAce.editor.undo();
    this.focusEditor();
  }

  handleSelectionChange = (selection) => {
    if (!this.reactAce || !this.props.toolbar || this.props.readOnly) {
      return;
    }
    const selectedText = this.reactAce.editor.getSession().getTextRange(selection.getRange());
    this.setState({ selectedText: selectedText });
  }

  focusEditor = () => {
    this.reactAce.editor.focus();
  }

  render() {
    const { height, width } = this.state;
    const { theme, resizable } = this.props;
    const validCssWidth = Number.isNaN(width) ? '100%' : width;
    const containerStyle = `${style.sourceCodeEditor} ${theme !== 'light' && style.darkMode} ${!resizable && style.static}`;
    const overlay = <Tooltip id={'paste-button-tooltip'}>Press Ctrl+V (&#8984;V in macOS) or select Edit&thinsp;&rarr;&thinsp;Paste to paste from clipboard.</Tooltip>;
    return (
      <div>
        {this.props.toolbar &&
          <div className={style.toolbar} style={{ width: validCssWidth }}>
            <ButtonToolbar>
              <ButtonGroup>
                <ClipboardButton title={<i className="fa fa-copy fa-fw" />}
                                 bsStyle="link"
                                 bsSize="sm"
                                 onSuccess={this.focusEditor}
                                 text={this.state.selectedText}
                                 buttonTitle="Copy (Ctrl+C / &#8984;C)"
                                 disabled={this.isCopyDisabled()} />
                <OverlayTrigger placement="top" trigger="click" overlay={overlay} rootClose>
                  <Button bsStyle="link" bsSize="sm" title="Paste (Ctrl+V / &#8984;V)" disabled={this.isPasteDisabled()}>
                    <i className="fa fa-paste fa-fw" />
                  </Button>
                </OverlayTrigger>
              </ButtonGroup>
              <ButtonGroup>
                <Button bsStyle="link"
                        bsSize="sm"
                        onClick={this.handleUndo}
                        title="Undo (Ctrl+Z / &#8984;Z)"
                        disabled={this.isUndoDisabled()}>
                  <i className="fa fa-undo fa-fw" />
                </Button>
                <Button bsStyle="link"
                        bsSize="sm"
                        onClick={this.handleRedo}
                        title="Redo (Ctrl+Shift+Z / &#8984;&#8679;Z)"
                        disabled={this.isRedoDisabled()}>
                  <i className="fa fa-repeat fa-fw" />
                </Button>
              </ButtonGroup>
            </ButtonToolbar>
          </div>
        }
        <Resizable height={height}
                   width={width}
                   minConstraints={[200, 200]}
                   onResize={this.handleResize}>
          <div className={containerStyle} style={{ height: height, width: validCssWidth }}>
            <AceEditor ref={(c) => { this.reactAce = c; }}
                       annotations={this.props.annotations}
                       editorProps={{ $blockScrolling: 'Infinity' }}
                       focus={this.props.focus}
                       fontSize={this.props.fontSize}
                       mode={this.props.mode}
                       theme={this.props.theme === 'light' ? 'tomorrow' : 'monokai'}
                       name={this.props.id}
                       height="100%"
                       onLoad={this.props.onLoad}
                       onChange={this.props.onChange}
                       onSelectionChange={this.handleSelectionChange}
                       readOnly={this.props.readOnly}
                       value={this.props.value}
                       width="100%" />
          </div>
        </Resizable>
      </div>
    );
  }
}

export default SourceCodeEditor;
