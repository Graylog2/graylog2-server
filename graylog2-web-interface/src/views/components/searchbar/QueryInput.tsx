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
import { useCallback, useMemo, useRef } from 'react';
import type { DefaultTheme } from 'styled-components';
import { withTheme } from 'styled-components';
import PropTypes from 'prop-types';

import { themePropTypes } from 'theme';
import withPluginEntities from 'views/logic/withPluginEntities';
import UserPreferencesContext from 'contexts/UserPreferencesContext';
import type { TimeRange, NoTimeRangeOverride } from 'views/logic/queries/Query';
import QueryValidationActions from 'views/actions/QueryValidationActions';
import type { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';

import type ReactAce from './queryinput/ace';
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

const handleExecution = (editor: Editor, onExecute: (query: string) => void, value: string, error: QueryValidationState | undefined) => {
  if (editor?.completer && editor.completer.popup) {
    editor.completer.popup.hide();
  }

  if (error) {
    QueryValidationActions.displayValidationErrors();

    return;
  }

  onExecute(value);
};

const _configureEditor = (editor, completer: AutoCompleter, configuredListeners: React.MutableRefObject<boolean>) => {
  if (editor) {
    editor.commands.removeCommands(['find', 'indent', 'outdent']);
    // eslint-disable-next-line no-param-reassign
    editor.completers = [completer];

    if (!configuredListeners.current) {
      editor.session.on('tokenizerUpdate', (input, { bgTokenizer: { currentLine, lines } }) => {
        if (completer.shouldShowCompletions(currentLine, lines)) {
          editor.execCommand('startAutocomplete');
        }
      });

      // eslint-disable-next-line no-param-reassign
      configuredListeners.current = true;
    }
  }
};

const getMarkers = (errors: QueryValidationState | undefined, warnings: QueryValidationState | undefined) => {
  const markerClassName = 'ace_marker';
  const createMarkers = (explanations = [], className) => explanations.map(({
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
  onExecute,
  placeholder,
  streams,
  timeRange,
  theme,
  value,
  warning,
}: Props) => {
  const completer = useMemo(() => completerFactory(completers, timeRange, streams), [completerFactory, completers, timeRange, streams]);
  const configuredListeners = useRef<boolean>(false);
  const configureEditor = useCallback((node: ReactAce) => _configureEditor(node?.editor, completer, configuredListeners), [completer]);
  const _onExecute = useCallback((editor: Editor) => handleExecution(editor, onExecute, value, error), [onExecute, value, error]);
  const markers = useMemo(() => getMarkers(error, warning), [error, warning]);

  return (
    <UserPreferencesContext.Consumer>
      {({ enableSmartSearch = true }) => (
        <StyledAceEditor mode="lucene"
                         disabled={disabled}
                         aceTheme="ace-queryinput" // NOTE: is usually just `theme` but we need that prop for styled-components
                         ref={configureEditor}
                         onLoad={(editor) => { editor.renderer.setScrollMargin(6, 5); editor.renderer.setPadding(12); }}
                         readOnly={disabled}
                         onBlur={onBlur}
                         commands={[{
                           name: 'Execute',
                           bindKey: 'Enter',
                           exec: _onExecute,
                         }]}
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
