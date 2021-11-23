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
import { withTheme, DefaultTheme } from 'styled-components';
import PropTypes from 'prop-types';

import { themePropTypes } from 'theme';
import withPluginEntities from 'views/logic/withPluginEntities';
import UserPreferencesContext from 'contexts/UserPreferencesContext';
import { QueriesActions } from 'views/stores/QueriesStore';

import type { AutoCompleter, Editor } from './ace-types';
import StyledAceEditor from './queryinput/StyledAceEditor';
import SearchBarAutoCompletions from './SearchBarAutocompletions';
import type { Completer } from './SearchBarAutocompletions';

type Props = {
  className?: string
  completerFactory: (completers: Array<Completer>) => AutoCompleter,
  completers: Array<Completer>,
  disabled?: boolean,
  error: string,
  height?: number,
  onBlur?: (query: string) => void,
  onChange: (query: string) => Promise<string>,
  onExecute: (query: string) => void,
  placeholder?: string,
  theme: DefaultTheme,
  value: string,
};

const defaultCompleterFactory = (completers) => new SearchBarAutoCompletions(completers);

const handleExecution = (editor, onExecute, value, error) => {
  if (editor.completer && editor.completer.popup) {
    editor.completer.popup.hide();
  }

  if (error) {
    QueriesActions.displayValidationErrors();

    return;
  }

  onExecute(value);
};

const _configureEditor = (node, completer, onExecute) => {
  const editor = node && node.editor;

  if (editor) {
    editor.commands.addCommands([{
      name: 'Execute',
      bindKey: 'Enter',
      exec: onExecute,
    },
    {
      name: 'SuppressShiftEnter',
      bindKey: 'Shift-Enter',
      exec: () => {},
    }]);

    editor.commands.removeCommands(['find', 'indent', 'outdent']);
    editor.completers = [completer];
  }
};

const QueryInput = ({
  className,
  completerFactory = defaultCompleterFactory,
  completers,
  disabled,
  error,
  height,
  onBlur,
  onChange,
  onExecute,
  placeholder,
  theme,
  value,
}: Props) => {
  const completer = useMemo(() => completerFactory(completers), [completerFactory, completers]);
  const _onExecute = useCallback((editor: Editor) => handleExecution(editor, onExecute, value, error), [onExecute, value, error]);
  const configureEditor = useCallback((node) => _configureEditor(node, completer, _onExecute), [completer, _onExecute]);

  return (
    <div className={`query ${className}`} style={{ display: 'flex' }} data-testid="query-input">
      <UserPreferencesContext.Consumer>
        {({ enableSmartSearch = true }) => (
          <StyledAceEditor mode="lucene"
                           disabled={disabled}
                           aceTheme="ace-queryinput" // NOTE: is usually just `theme` but we need that prop for styled-components
                           ref={configureEditor}
                           readOnly={disabled}
                           onBlur={onBlur}
                           onChange={onChange}
                           value={value}
                           name="QueryEditor"
                           showGutter={false}
                           showPrintMargin={false}
                           highlightActiveLine={false}
                           minLines={1}
                           maxLines={1}
                           enableBasicAutocompletion={enableSmartSearch}
                           enableLiveAutocompletion={enableSmartSearch}
                           editorProps={{
                             $blockScrolling: Infinity,
                             selectionStyle: 'line',
                           }}
                           fontSize={theme.fonts.size.large}
                           placeholder={placeholder}
                           $height={height} />
        )}
      </UserPreferencesContext.Consumer>
    </div>
  );
};

QueryInput.propTypes = {
  className: PropTypes.string,
  completerFactory: PropTypes.func,
  completers: PropTypes.array,
  disabled: PropTypes.bool,
  error: PropTypes.object,
  height: PropTypes.number,
  onBlur: PropTypes.func,
  onChange: PropTypes.func.isRequired,
  onExecute: PropTypes.func.isRequired,
  placeholder: PropTypes.string,
  theme: themePropTypes.isRequired,
  value: PropTypes.string,
};

QueryInput.defaultProps = {
  className: '',
  completerFactory: defaultCompleterFactory,
  completers: [],
  disabled: false,
  error: undefined,
  height: undefined,
  onBlur: () => {},
  placeholder: '',
  value: '',
};

export default withPluginEntities(withTheme(QueryInput), { completers: 'views.completers' });
