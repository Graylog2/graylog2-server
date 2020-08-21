// @flow strict
import * as React from 'react';
import { useCallback, useMemo } from 'react';
import { withTheme } from 'styled-components';
import PropTypes from 'prop-types';

import type { ThemeInterface } from 'theme';
import withPluginEntities from 'views/logic/withPluginEntities';
import UserPreferencesContext from 'contexts/UserPreferencesContext';

import type { AutoCompleter, Editor } from './ace-types';
import StyledAceEditor from './queryinput/StyledAceEditor';
import SearchBarAutoCompletions from './SearchBarAutocompletions';
import type { Completer } from './SearchBarAutocompletions';

type Props = {|
  completerFactory: (Array<Completer>) => AutoCompleter,
  completers: Array<Completer>,
  disabled: boolean,
  onBlur?: (string) => void,
  onChange: (string) => Promise<string>,
  onExecute: (string) => void,
  placeholder: string,
  theme: ThemeInterface,
  value: string,
|};

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
  completers: PropTypes.array,
  completerFactory: PropTypes.func,
  disabled: PropTypes.bool,
  onBlur: PropTypes.func,
  onChange: PropTypes.func.isRequired,
  onExecute: PropTypes.func.isRequired,
  placeholder: PropTypes.string,
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

export default withTheme(withPluginEntities(QueryInput, { completers: 'views.completers' }));
