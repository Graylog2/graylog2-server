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
import * as React from 'react';
import { useCallback, useMemo } from 'react';
import type { DefaultTheme } from 'styled-components';
import { withTheme } from 'styled-components';
import PropTypes from 'prop-types';

import { themePropTypes } from 'theme';
import withPluginEntities from 'views/logic/withPluginEntities';
import UserPreferencesContext from 'contexts/UserPreferencesContext';
import type { TimeRange, NoTimeRangeOverride } from 'views/logic/queries/Query';
import QueryValidationActions from 'views/actions/QueryValidationActions';
import type { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';

import type { AutoCompleter, Editor } from './ace-types';
import StyledAceEditor from './queryinput/StyledAceEditor';
import SearchBarAutoCompletions from './SearchBarAutocompletions';
import type { Completer } from './SearchBarAutocompletions';

type Props = {
  className?: string
  completerFactory: (
    completers: Array<Completer>,
    timeRange: TimeRange | NoTimeRangeOverride | undefined,
    streams: Array<string>,
  ) => AutoCompleter,
  completers: Array<Completer>,
  disabled?: boolean,
  disableExecution?: boolean,
  error?: QueryValidationState,
  onBlur?: (query: string) => void,
  onChange: (query: string) => Promise<string>,
  onExecute: (query: string) => void,
  placeholder?: string,
  streams?: Array<string>,
  timeRange?: TimeRange | NoTimeRangeOverride,
  theme: DefaultTheme,
  value: string,
  warning?: QueryValidationState,
};

const defaultCompleterFactory = (
  completers: Array<Completer>,
  timeRange: TimeRange | NoTimeRangeOverride,
  streams: Array<string>,
) => new SearchBarAutoCompletions(completers, timeRange, streams);

const handleExecution = (editor: Editor, onExecute: (query: string) => void, value: string, error: QueryValidationState | undefined, disableExecution: boolean) => {
  if (error) {
    QueryValidationActions.displayValidationErrors();

    return;
  }

  if (disableExecution) {
    return;
  }

  if (editor?.completer && editor.completer.popup) {
    editor.completer.popup.hide();
  }

  onExecute(value);
};

// This function takes care of all editor configuration options, which do not rely on external data.
// It will only run once, on mount, which is important for e.g. the event listeners.
const _onLoadEditor = (editor) => {
  if (editor) {
    editor.commands.removeCommands(['find', 'indent', 'outdent']);

    editor.session.on('tokenizerUpdate', (input, { bgTokenizer: { currentLine, lines } }) => {
      editor.completers.forEach((completer) => {
        if (completer?.shouldShowCompletions(currentLine, lines)) {
          editor.execCommand('startAutocomplete');
        }
      });
    });

    editor.renderer.setScrollMargin(6, 5);
    editor.renderer.setPadding(12);
  }
};

// This function takes care of updating the editor config on every render.
// This is necessary for configuration options which rely on external data.
// Unfortunately it is not possible to configure for example the command once
// with the `onLoad` or `commands` prop, because the reference for the related function will be outdated.
const _updateEditorConfiguration = (node, completer, onExecute) => {
  const editor = node && node.editor;

  if (editor) {
    editor.commands.addCommand({
      name: 'Execute',
      bindKey: { win: 'Enter', mac: 'Enter' },
      exec: onExecute,
    });

    editor.completers = [completer];
  }
};

const getMarkers = (errors: QueryValidationState | undefined, warnings: QueryValidationState | undefined) => {
  const markerClassName = 'ace_marker';
  const createMarkers = (explanations = [], className = '') => explanations.map(({
    beginLine,
    beginColumn,
    endLine,
    endColumn,
  }) => ({
    startRow: beginLine,
    startCol: beginColumn,
    endRow: endLine,
    endCol: endColumn,
    type: 'background',
    className,
  }));

  return [
    ...createMarkers(errors?.explanations, `${markerClassName} ace_validation_error`),
    ...createMarkers(warnings?.explanations, `${markerClassName} ace_validation_warning`),
  ];
};

const QueryInput = ({
  className,
  completerFactory = defaultCompleterFactory,
  completers,
  disabled,
  error,
  onBlur,
  onChange,
  onExecute: onExecuteProp,
  placeholder,
  streams,
  timeRange,
  theme,
  value,
  warning,
  disableExecution,
}: Props) => {
  const completer = useMemo(() => completerFactory(completers, timeRange, streams), [completerFactory, completers, timeRange, streams]);
  const onLoadEditor = useCallback((editor: Editor) => _onLoadEditor(editor), []);
  const onExecute = useCallback((editor: Editor) => handleExecution(editor, onExecuteProp, value, error, disableExecution), [onExecuteProp, value, error, disableExecution]);
  const markers = useMemo(() => getMarkers(error, warning), [error, warning]);
  const updateEditorConfiguration = useCallback((node) => _updateEditorConfiguration(node, completer, onExecute), [onExecute, completer]);

  return (
    <UserPreferencesContext.Consumer>
      {({ enableSmartSearch = true }) => (
        <StyledAceEditor mode="lucene"
                         disabled={disabled}
                         className={className}
                         aceTheme="ace-queryinput" // NOTE: is usually just `theme` but we need that prop for styled-components
                         ref={updateEditorConfiguration}
                         onLoad={onLoadEditor}
                         readOnly={disabled}
                         onBlur={onBlur}
                         onChange={onChange}
                         value={value}
                         name="QueryEditor"
                         showGutter={false}
                         showPrintMargin={false}
                         highlightActiveLine={false}
                         minLines={1}
                         maxLines={4}
                         enableBasicAutocompletion={enableSmartSearch}
                         enableLiveAutocompletion={enableSmartSearch}
                         editorProps={{
                           $blockScrolling: Infinity,
                           selectionStyle: 'line',
                         }}
                         setOptions={{
                           indentedSoftWrap: false,
                         }}
                         fontSize={theme.fonts.size.large}
                         placeholder={placeholder}
                         markers={markers}
                         wrapEnabled />
      )}
    </UserPreferencesContext.Consumer>
  );
};

QueryInput.propTypes = {
  className: PropTypes.string,
  completerFactory: PropTypes.func,
  completers: PropTypes.array,
  disabled: PropTypes.bool,
  disableExecution: PropTypes.bool,
  error: PropTypes.object,
  onBlur: PropTypes.func,
  onChange: PropTypes.func.isRequired,
  onExecute: PropTypes.func.isRequired,
  placeholder: PropTypes.string,
  streams: PropTypes.array,
  theme: themePropTypes.isRequired,
  timeRange: PropTypes.object,
  value: PropTypes.string,
  warning: PropTypes.object,
};

QueryInput.defaultProps = {
  className: '',
  completerFactory: defaultCompleterFactory,
  completers: [],
  disableExecution: false,
  disabled: false,
  error: undefined,
  onBlur: () => {},
  placeholder: '',
  streams: undefined,
  timeRange: undefined,
  value: '',
  warning: undefined,
};

export default withPluginEntities(withTheme(QueryInput), { completers: 'views.completers' });
