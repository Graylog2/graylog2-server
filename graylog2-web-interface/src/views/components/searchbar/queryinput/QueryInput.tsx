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
import { useCallback, useMemo, useContext, useRef } from 'react';
import PropTypes from 'prop-types';
import { isEmpty } from 'lodash';
import type { FormikErrors } from 'formik';

import UserPreferencesContext from 'contexts/UserPreferencesContext';
import type { TimeRange, NoTimeRangeOverride } from 'views/logic/queries/Query';
import QueryValidationActions from 'views/actions/QueryValidationActions';
import type { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';
import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';
import { DEFAULT_TIMERANGE } from 'views/Constants';
import { isNoTimeRangeOverride } from 'views/typeGuards/timeRange';
import usePluginEntities from 'hooks/usePluginEntities';
import useUserDateTime from 'hooks/useUserDateTime';

import type { AutoCompleter, Editor } from './ace-types';
import type { BaseProps } from './BasicQueryInput';
import BasicQueryInput from './BasicQueryInput';

import SearchBarAutoCompletions from '../SearchBarAutocompletions';
import type { Completer, FieldTypes } from '../SearchBarAutocompletions';

const defaultCompleterFactory = (...args: ConstructorParameters<typeof SearchBarAutoCompletions>) => new SearchBarAutoCompletions(...args);

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
      editor.completer.popup.hide();
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
const _onLoadEditor = (editor, isInitialTokenizerUpdate: React.MutableRefObject<boolean>) => {
  if (editor) {
    editor.commands.removeCommands(['find', 'indent', 'outdent']);

    editor.session.on('tokenizerUpdate', (_input, { bgTokenizer: { currentLine, lines } }) => {
      if (!isInitialTokenizerUpdate.current) {
        editor.completers.forEach((completer) => {
          if (completer?.shouldShowCompletions(currentLine, lines)) {
            editor.execCommand('startAutocomplete');
          }
        });
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

const useCompleter = ({ streams, timeRange, completerFactory, userTimezone }: Pick<Props, 'streams' | 'timeRange' | 'completerFactory'> & { userTimezone: string }) => {
  const completers = usePluginEntities('views.completers') ?? [];
  const { data: queryFields } = useFieldTypes(streams, isNoTimeRangeOverride(timeRange) ? DEFAULT_TIMERANGE : timeRange);
  const { data: allFields } = useFieldTypes([], DEFAULT_TIMERANGE);
  const fieldTypes = useMemo(() => {
    const queryFieldsByName = Object.fromEntries((queryFields ?? []).map((field) => [field.name, field]));
    const allFieldsByName = Object.fromEntries((allFields ?? []).map((field) => [field.name, field]));

    return { all: allFieldsByName, query: queryFieldsByName };
  }, [allFields, queryFields]);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  return useMemo(() => completerFactory(completers, timeRange, streams, fieldTypes, userTimezone), [completerFactory, timeRange, streams, fieldTypes, userTimezone]);
};

type Props = BaseProps & {
  completerFactory?: (
    completers: Array<Completer>,
    timeRange: TimeRange | NoTimeRangeOverride | undefined,
    streams: Array<string>,
    fieldTypes: FieldTypes,
    userTimezone: string,
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
};

const QueryInput = ({
  className,
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
}: Props) => {
  const { userTimezone } = useUserDateTime();
  const isInitialTokenizerUpdate = useRef(true);
  const { enableSmartSearch } = useContext(UserPreferencesContext);
  const completer = useCompleter({ streams, timeRange, completerFactory, userTimezone });
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
  const updateEditorConfiguration = useCallback((node) => _updateEditorConfiguration(node, completer, onExecute), [onExecute, completer]);
  const _onChange = useCallback((newQuery) => {
    onChange({ target: { value: newQuery, name } });

    return Promise.resolve(newQuery);
  }, [name, onChange]);

  return (
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
  );
};

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
};

export default QueryInput;
