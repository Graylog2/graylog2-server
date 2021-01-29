/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import lodash from 'lodash';
import { PropTypes } from 'prop-types';
import { Resizable } from 'react-resizable';
import AceEditor from 'react-ace';
import styled, { css } from 'styled-components';

import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import { Button, ButtonGroup, ButtonToolbar, OverlayTrigger, Tooltip } from 'components/graylog';
import PipelineRulesMode from 'components/rules/mode-pipeline';

import ClipboardButton from './ClipboardButton';
import Icon from './Icon';
import './webpack-resolver';
import './ace/theme-graylog';

const SourceCodeContainer = styled.div(({ resizable, theme }) => css`
  .react-resizable-handle {
    z-index: 100; /* Ensure resize handle is over text editor */
    display: ${resizable ? 'block' : 'none'};
  }

  ${theme.components.aceEditor}
`);

const StyledTooltip = styled(Tooltip)`
  //width: 250px;
`;

const Toolbar = styled.div(({ theme }) => css`
  background: ${theme.colors.global.contentBackground};
  border: 1px solid ${theme.colors.gray[80]};
  border-bottom: 0;
  border-radius: 5px 5px 0 0;

  .btn-link {
    color: ${theme.colors.variant.dark.info};
    
    :hover {
      color: ${theme.colors.variant.darkest.info};
      background-color: ${theme.colors.variant.lightest.info};
    }

    &.disabled,
    &[disabled] {
      color: ${theme.colors.variant.light.default};
      
      :hover {
        color: ${theme.colors.variant.light.default};
      }
    }
  }

  & + ${SourceCodeContainer} {
    /* Do not add border radius if code editor comes after toolbar */
    .ace_editor {
      border-radius: 0 0 5px 5px;
    }
  }
`);

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
    /** Provides a ref associated to AceEditor component */
    innerRef: PropTypes.oneOfType([
      PropTypes.func,
      PropTypes.shape({ current: PropTypes.any }),
    ]),
    /** Specifies the mode to use in the editor. This is used for highlighting and auto-completion. */
    mode: PropTypes.oneOf(['json', 'lua', 'markdown', 'text', 'yaml', 'pipeline']),
    /** Function called on editor load. The first argument is the instance of the editor. */
    onLoad: PropTypes.func,
    /** Function called when the value of the text changes. It receives the new value and an event as arguments. */
    onChange: PropTypes.func,
    /** Specifies if the editor should be in read-only mode. */
    readOnly: PropTypes.bool,
    /** Specifies if the editor should be resizable by the user. */
    resizable: PropTypes.bool,
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
    innerRef: undefined,
    mode: 'text',
    onChange: () => {},
    onLoad: () => {},
    readOnly: false,
    resizable: true,
    toolbar: true,
    value: undefined,
    width: Infinity,
  };

  constructor(props) {
    super(props);

    this.state = {
      height: props.height,
      width: props.width,
      selectedText: '',
    };

    this.overlayContainerRef = React.createRef();
  }

  componentDidMount() {
    const { mode } = this.props;

    if (mode === 'pipeline') {
      const url = qualifyUrl(ApiRoutes.RulesController.functions().url);

      fetch('GET', url).then((response) => {
        const functions = response.map((res) => res.name).join('|');
        const pipelineRulesMode = new PipelineRulesMode(functions);

        this.reactAce.editor.getSession().setMode(pipelineRulesMode);

        return functions;
      });
    }
  }

  componentDidUpdate(prevProps) {
    const { height, width } = this.props;

    if (height !== prevProps.height || width !== prevProps.width) {
      this.reloadEditor();
    }
  }

  handleResize = (event, { size }) => {
    const { height, width } = size;

    this.setState({ height: height, width: width }, this.reloadEditor);
  };

  reloadEditor = () => {
    const { resizable } = this.props;

    if (resizable) {
      this.reactAce.editor.resize();
    }
  };

  /* eslint-disable-next-line react/destructuring-assignment */
  isCopyDisabled = () => this.props.readOnly || this.state.selectedText === '';

  /* eslint-disable-next-line react/destructuring-assignment */
  isPasteDisabled = () => this.props.readOnly;

  /* eslint-disable-next-line react/destructuring-assignment */
  isRedoDisabled = () => this.props.readOnly || !this.reactAce || !this.reactAce.editor.getSession().getUndoManager().hasRedo();

  /* eslint-disable-next-line react/destructuring-assignment */
  isUndoDisabled = () => this.props.readOnly || !this.reactAce || !this.reactAce.editor.getSession().getUndoManager().hasUndo();

  handleRedo = () => {
    this.reactAce.editor.redo();
    this.focusEditor();
  };

  handleUndo = () => {
    this.reactAce.editor.undo();
    this.focusEditor();
  };

  handleSelectionChange = (selection) => {
    const { toolbar, readOnly } = this.props;

    if (!this.reactAce || !toolbar || readOnly) {
      return;
    }

    const selectedText = this.reactAce.editor.getSession().getTextRange(selection.getRange());

    this.setState({ selectedText: selectedText });
  };

  focusEditor = () => {
    this.reactAce.editor.focus();
  };

  render() {
    const { height, width, selectedText } = this.state;
    const {
      resizable,
      toolbar,
      annotations,
      focus,
      fontSize,
      mode,
      id,
      innerRef,
      onLoad,
      onChange,
      readOnly,
      value,
    } = this.props;
    const validCssWidth = lodash.isFinite(width) ? width : '100%';
    const overlay = <StyledTooltip id="paste-button-tooltip" className="in">Press Ctrl+V (&#8984;V in macOS) or select Edit&thinsp;&rarr;&thinsp;Paste to paste from clipboard.</StyledTooltip>;

    return (
      <div>
        {toolbar
          && (
          <Toolbar style={{ width: validCssWidth }}>
            <ButtonToolbar>
              <ButtonGroup ref={this.overlayContainerRef}>
                <ClipboardButton title={<Icon name="copy" fixedWidth />}
                                 bsStyle="link"
                                 bsSize="sm"
                                 onSuccess={this.focusEditor}
                                 text={selectedText}
                                 buttonTitle="Copy (Ctrl+C / &#8984;C)"
                                 disabled={this.isCopyDisabled()} />
                <OverlayTrigger placement="top" trigger="click" overlay={overlay} rootClose container={this.overlayContainerRef.current}>
                  <Button bsStyle="link" bsSize="sm" title="Paste (Ctrl+V / &#8984;V)" disabled={this.isPasteDisabled()}>
                    <Icon name="clipboard" fixedWidth />
                  </Button>
                </OverlayTrigger>
              </ButtonGroup>
              <ButtonGroup>
                <Button bsStyle="link"
                        bsSize="sm"
                        onClick={this.handleUndo}
                        title="Undo (Ctrl+Z / &#8984;Z)"
                        disabled={this.isUndoDisabled()}>
                  <Icon name="undo" fixedWidth />
                </Button>
                <Button bsStyle="link"
                        bsSize="sm"
                        onClick={this.handleRedo}
                        title="Redo (Ctrl+Shift+Z / &#8984;&#8679;Z)"
                        disabled={this.isRedoDisabled()}>
                  <Icon name="redo" fixedWidth />
                </Button>
              </ButtonGroup>
            </ButtonToolbar>
          </Toolbar>
          )}
        <Resizable height={height}
                   width={width}
                   minConstraints={[200, 200]}
                   onResize={this.handleResize}>
          <SourceCodeContainer style={{ height: height, width: validCssWidth }}
                               resizable={resizable}>
            <AceEditor ref={(c) => {
              this.reactAce = c;
              if (innerRef) { innerRef.current = c; }
            }}
                       annotations={annotations}
                       editorProps={{ $blockScrolling: 'Infinity' }}
                       // Convert Windows line breaks to Unix. See issue #7889
                       setOptions={{ newLineMode: 'unix' }}
                       focus={focus}
                       fontSize={fontSize}
                       mode={mode}
                       theme="graylog"
                       name={id}
                       height="100%"
                       onLoad={onLoad}
                       onChange={onChange}
                       onSelectionChange={this.handleSelectionChange}
                       readOnly={readOnly}
                       value={value}
                       width="100%" />
          </SourceCodeContainer>
        </Resizable>
      </div>
    );
  }
}

export default SourceCodeEditor;
