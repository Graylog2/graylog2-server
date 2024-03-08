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
import { useCallback, useMemo, useContext, useRef, useImperativeHandle } from 'react';
import PropTypes from 'prop-types';
import isEmpty from 'lodash/isEmpty';
import type { FormikErrors } from 'formik';
import styled, { createGlobalStyle, css } from 'styled-components';

import UserPreferencesContext from 'contexts/UserPreferencesContext';
import type { TimeRange, NoTimeRangeOverride } from 'views/logic/queries/Query';
import QueryValidationActions from 'views/actions/QueryValidationActions';
import type { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';
import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';
import { DEFAULT_TIMERANGE } from 'views/Constants';
import { isNoTimeRangeOverride } from 'views/typeGuards/timeRange';
import usePluginEntities from 'hooks/usePluginEntities';
import useUserDateTime from 'hooks/useUserDateTime';
import type View from 'views/logic/views/View';
import useElementDimensions from 'hooks/useElementDimensions';
import { displayHistoryCompletions } from 'views/components/searchbar/QueryHistoryButton';
import { startAutocomplete } from 'views/components/searchbar/queryinput/commands';
import useHotkey from 'hooks/useHotkey';

import type { AutoCompleter, Editor, Command } from './ace-types';
import type { BaseProps } from './BasicQueryInput';
import BasicQueryInput from './BasicQueryInput';

import SearchBarAutoCompletions from '../SearchBarAutocompletions';
import type { Completer, FieldTypes } from '../SearchBarAutocompletions';

const defaultCompleterFactory = (...args: ConstructorParameters<typeof SearchBarAutoCompletions>) => new SearchBarAutoCompletions(...args);

const GlobalEditorStyles = createGlobalStyle<{ $width?: number; $offsetLeft: number }>`
  .ace_editor.ace_autocomplete {
    width: ${(props) => (props.$width ?? 600) - 12}px !important;
    left: ${(props) => (props.$offsetLeft ?? 143) + 7}px !important;
  }
`;

const Container = styled.div<{ $hasValue: boolean }>(({ $hasValue }) => css`
  width: 100%;

  .ace_hidden-cursors {
    display: ${$hasValue ? 'block' : 'none'};
  }
`);

const displayValidationErrors = () => {
  QueryValidationActions.displayValidationErrors();
};

const handleExecution = ({
  editor,
  onExecute,
  value,
  error,
  disableExecution,
  isValidating,
  validate,
}: {
  editor: Editor,
  onExecute: (query: string) => void,
  value: string,
  error: QueryValidationState | undefined,
  disableExecution: boolean,
  isValidating: boolean,
  validate: () => Promise<FormikErrors<{}>>,
}) => {
  const execute = () => {
    if (editor?.completer && editor.completer.popup) {
      editor.completer.detach();
    }

    onExecute(value);
  };

  if (isValidating) {
    validate().then((errors) => {
      if (isEmpty(errors)) {
        execute();
      } else {
        displayValidationErrors();
      }
    });

    return;
  }

  if (error) {
    displayValidationErrors();
  }

  if (disableExecution || error) {
    return;
  }

  execute();
};

// This function takes care of all editor configuration options, which do not rely on external data.
// It will only run once, on mount, which is important for e.g. the event listeners.
const _onLoadEditor = (editor: Editor, isInitialTokenizerUpdate: React.MutableRefObject<boolean>) => {
  if (editor) {
    editor.commands.removeCommands(['find', 'indent', 'outdent']);

    editor.session.on('tokenizerUpdate', () => {
      if (editor.isFocused() && !editor.completer?.activated && editor.getValue() && !isInitialTokenizerUpdate.current) {
        startAutocomplete(editor);
      }

      if (isInitialTokenizerUpdate.current) {
        // eslint-disable-next-line no-param-reassign
        isInitialTokenizerUpdate.current = false;
      }
    });
  }
};

// This function takes care of updating the editor config on every render.
// This is necessary for configuration options which rely on external data.
// Unfortunately it is not possible to configure for example the command once
// with the `onLoad` or `commands` prop, because the reference for the related function will be outdated.
const _updateEditorConfiguration = (node: { editor: Editor; }, completer: AutoCompleter, commands: Array<Command>, ref: React.MutableRefObject<Editor>) => {
  const editor = node?.editor;

  if (ref && editor) {
    // eslint-disable-next-line no-param-reassign
    ref.current = editor;
  }

  if (editor) {
    editor.commands.on('afterExec', () => {
      if (editor.completer?.autoSelect) {
        editor.completer.autoSelect = false;
      }

      const completerCommandKeyBinding = editor.completer?.keyboardHandler?.commandKeyBinding;

      if (completerCommandKeyBinding?.tab && completerCommandKeyBinding.tab.name !== 'improved-tab') {
        editor.completer.keyboardHandler.addCommand({
          name: 'improved-tab',
          bindKey: { win: 'Tab', mac: 'Tab' },
          exec: (currentEditor: Editor) => {
            const result = currentEditor.completer.insertMatch();

            if (!result && !currentEditor.tabstopManager) {
              currentEditor.completer.goTo('down');

              return currentEditor.completer.insertMatch();
            }

            return result;
          },
        });
      }
    });

    commands.forEach((command) => editor.commands.addCommand(command));
    editor.completers = [completer];
  }
};

const useCompleter = ({ streams, timeRange, completerFactory, view }: Pick<Props, 'streams' | 'timeRange' | 'completerFactory' | 'view'>) => {
  const { userTimezone } = useUserDateTime();
  const completers = usePluginEntities('views.completers');
  const { data: queryFields } = useFieldTypes(streams, isNoTimeRangeOverride(timeRange) ? DEFAULT_TIMERANGE : timeRange);
  const { data: allFields } = useFieldTypes([], DEFAULT_TIMERANGE);
  const fieldTypes = useMemo(() => {
    const queryFieldsByName = Object.fromEntries((queryFields ?? []).map((field) => [field.name, field]));
    const allFieldsByName = Object.fromEntries((allFields ?? []).map((field) => [field.name, field]));

    return { all: allFieldsByName, query: queryFieldsByName };
  }, [allFields, queryFields]);

  return useMemo(() => completerFactory(completers ?? [], timeRange, streams, fieldTypes, userTimezone, view),
    [completerFactory, completers, timeRange, streams, fieldTypes, userTimezone, view]);
};

const useShowHotkeysInOverview = () => {
  const options = { enabled: false };

  useHotkey({
    scope: 'query-input',
    actionKey: 'submit-search',
    options,
  });

  useHotkey({
    scope: 'query-input',
    actionKey: 'insert-newline',
    options,
  });

  useHotkey({
    scope: 'query-input',
    actionKey: 'create-search-filter',
    options,
  });

  useHotkey({
    scope: 'query-input',
    actionKey: 'show-suggestions',
    options,
  });

  useHotkey({
    scope: 'query-input',
    actionKey: 'show-history',
    options,
  });
};

type Props = BaseProps & {
  commands?: Array<Command>,
  completerFactory?: (
    completers: Array<Completer>,
    timeRange: TimeRange | NoTimeRangeOverride | undefined,
    streams: Array<string>,
    fieldTypes: FieldTypes,
    userTimezone: string,
    view: View | undefined,
  ) => AutoCompleter,
  disableExecution?: boolean,
  isValidating?: boolean,
  name: string,
  onBlur?: (query: string) => void,
  onChange: (changeEvent: { target: { value: string, name: string } }) => Promise<string>,
  onExecute: (query: string) => void,
  streams?: Array<string> | undefined,
  timeRange?: TimeRange | NoTimeRangeOverride | undefined,
  validate: () => Promise<FormikErrors<{}>>,
  view?: View
};

const QueryInput = React.forwardRef<Editor, Props>(({
  className,
  commands,
  completerFactory = defaultCompleterFactory,
  disableExecution,
  error,
  height,
  inputId,
  isValidating,
  maxLines,
  onBlur,
  onChange,
  onExecute: onExecuteProp,
  placeholder,
  streams,
  timeRange,
  value,
  validate,
  warning,
  wrapEnabled,
  name,
  view,
}, outerRef) => {
  const innerRef = useRef<Editor>(null);
  const inputElement = innerRef.current?.container;
  const { width: inputWidth } = useElementDimensions(inputElement);
  const isInitialTokenizerUpdate = useRef(true);
  const { enableSmartSearch } = useContext(UserPreferencesContext);
  const completer = useCompleter({ streams, timeRange, completerFactory, view });
  const onLoadEditor = useCallback((editor: Editor) => _onLoadEditor(editor, isInitialTokenizerUpdate), []);
  const onExecute = useCallback((editor: Editor) => handleExecution({
    editor,
    onExecute: onExecuteProp,
    value,
    error,
    disableExecution,
    isValidating,
    validate,
  }), [onExecuteProp, value, error, disableExecution, isValidating, validate]);
  const _commands = useMemo(() => [
    ...commands,
    {
      name: 'Execute',
      bindKey: { win: 'Enter', mac: 'Enter' },
      exec: onExecute,
    },
    {
      name: 'Show completions',
      bindKey: { win: 'Alt-Space', mac: 'Alt-Space' },
      exec: async (editor: Editor) => {
        if (editor.getValue()) {
          startAutocomplete(editor);

          return;
        }

        await displayHistoryCompletions(editor);
      },
    },
    {
      name: 'Show query history',
      bindKey: { win: 'Alt-Shift-H', mac: 'Alt-Shift-H' },
      exec: async (editor: Editor) => {
        displayHistoryCompletions(editor);
      },
    },
    // The following will disable the mentioned hotkeys.
    {
      name: 'Do nothing',
      bindKey: { win: 'Ctrl-Space|Ctrl-Shift-Space', mac: 'Ctrl-Space|Ctrl-Shift-Space' },
      exec: () => {},
    },
  ], [commands, onExecute]);
  const updateEditorConfiguration = useCallback((node: { editor: Editor }) => _updateEditorConfiguration(node, completer, _commands, innerRef), [_commands, completer]);
  const _onChange = useCallback((newQuery: string) => {
    onChange({ target: { value: newQuery, name } });

    return Promise.resolve(newQuery);
  }, [name, onChange]);

  useShowHotkeysInOverview();
  useImperativeHandle(outerRef, () => innerRef.current, []);

  const offsetLeft = useMemo(() => inputElement?.getBoundingClientRect()?.left, [inputElement]);

  return (
    <Container $hasValue={!!value}>
      <GlobalEditorStyles $width={inputWidth} $offsetLeft={offsetLeft} />
      <BasicQueryInput height={height}
                       className={className}
                       disabled={false}
                       enableAutocompletion={enableSmartSearch}
                       error={error}
                       inputId={inputId}
                       warning={warning}
                       maxLines={maxLines}
                       onBlur={onBlur}
                       onExecute={onExecute}
                       onChange={_onChange}
                       onLoad={onLoadEditor}
                       placeholder={placeholder}
                       ref={updateEditorConfiguration}
                       value={value}
                       wrapEnabled={wrapEnabled} />

    </Container>
  );
});

QueryInput.propTypes = {
  className: PropTypes.string,
  completerFactory: PropTypes.func,
  disableExecution: PropTypes.bool,
  error: PropTypes.any,
  inputId: PropTypes.string,
  height: PropTypes.number,
  isValidating: PropTypes.bool.isRequired,
  maxLines: PropTypes.number,
  onBlur: PropTypes.func,
  onChange: PropTypes.func.isRequired,
  onExecute: PropTypes.func.isRequired,
  placeholder: PropTypes.string,
  streams: PropTypes.array,
  timeRange: PropTypes.object,
  value: PropTypes.string,
  warning: PropTypes.any,
  wrapEnabled: PropTypes.bool,
  validate: PropTypes.func.isRequired,
};

QueryInput.defaultProps = {
  className: '',
  commands: [],
  completerFactory: defaultCompleterFactory,
  disableExecution: false,
  error: undefined,
  height: undefined,
  inputId: undefined,
  maxLines: undefined,
  onBlur: undefined,
  placeholder: '',
  streams: undefined,
  timeRange: undefined,
  value: '',
  warning: undefined,
  wrapEnabled: undefined,
  view: undefined,
};

export default QueryInput;
