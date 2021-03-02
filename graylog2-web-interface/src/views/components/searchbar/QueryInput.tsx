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

import type { AutoCompleter, Editor } from './ace-types';
import StyledAceEditor from './queryinput/StyledAceEditor';
import SearchBarAutoCompletions from './SearchBarAutocompletions';
import type { Completer } from './SearchBarAutocompletions';

type Props = {
  completerFactory: (completers: Array<Completer>) => AutoCompleter,
  completers: Array<Completer>,
  disabled?: boolean,
  onBlur?: (query: string) => void,
  onChange: (query: string) => Promise<string>,
  onExecute: (query: string) => void,
  placeholder?: string,
  theme: DefaultTheme,
  value: string,
};

const defaultCompleterFactory = (completers) => new SearchBarAutoCompletions(completers);

const QueryInput = ({ disabled, onBlur, onChange, onExecute, placeholder, value, completers, completerFactory = defaultCompleterFactory, theme }: Props) => {
  const completer = useMemo(() => completerFactory(completers), [completerFactory, completers]);
  const _onExecute = useCallback((editor: Editor) => {
    if (editor.completer && editor.completer.popup) {
      editor.completer.popup.hide();
    }

    onExecute(value);
  }, [onExecute]);

  const editorRef = useCallback((node) => {
    const editor = node && node.editor;

    if (editor) {
      editor.commands.addCommand({
        name: 'Execute',
        bindKey: { win: 'Enter', mac: 'Enter' },
        exec: _onExecute,
      });

      editor.commands.removeCommands(['indent', 'outdent']);
      editor.completers = [completer];
    }
  }, [completer, _onExecute]);

  return (
    <div className="query" style={{ display: 'flex' }} data-testid="query-input">
      <UserPreferencesContext.Consumer>
        {({ enableSmartSearch = true }) => (
          <StyledAceEditor mode="lucene"
                           disabled={disabled}
                           aceTheme="ace-queryinput" // NOTE: is usually just `theme` but we need that prop for styled-components
                           ref={editorRef}
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
                           placeholder={placeholder} />
        )}
      </UserPreferencesContext.Consumer>
    </div>
  );
};

QueryInput.propTypes = {
  completerFactory: PropTypes.func,
  completers: PropTypes.array,
  disabled: PropTypes.bool,
  onBlur: PropTypes.func,
  onChange: PropTypes.func.isRequired,
  onExecute: PropTypes.func.isRequired,
  placeholder: PropTypes.string,
  theme: themePropTypes.isRequired,
  value: PropTypes.string,
};

QueryInput.defaultProps = {
  disabled: false,
  onBlur: () => {},
  completers: [],
  completerFactory: defaultCompleterFactory,
  value: '',
  placeholder: '',
};

export default withPluginEntities(withTheme(QueryInput), { completers: 'views.completers' });
