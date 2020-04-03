// @flow strict
import { sortBy, uniqBy } from 'lodash';
import type { Editor, ResultsCallback, Session, Position, CompletionResult, AutoCompleter, Token } from './ace-types';

export interface Completer {
  getCompletions(currentToken: ?Token, lastToken: ?Token, prefix: string, tokens: Array<Token>, currentTokenIdx: number): Array<CompletionResult>;
}

export default class SearchBarAutoCompletions implements AutoCompleter {
  completers: Array<Completer>;

  constructor(completers: Array<Completer> = []) {
    this.completers = completers;
  }

  getCompletions = (editor: Editor, session: Session, pos: Position, prefix: string, callback: ResultsCallback) => {
    // eslint-disable-next-line no-param-reassign
    editor.completer.autoSelect = false;
    const tokens = editor.session.getTokens(pos.row);
    const currentToken = editor.session.getTokenAt(pos.row, pos.column);
    const currentTokenIdx = tokens.findIndex((t) => (t === currentToken));

    const lastToken: ?Token = currentTokenIdx > 0 ? tokens[currentTokenIdx - 1] : null;

    const results = this.completers
      .map((completer) => {
        try {
          return completer.getCompletions(currentToken, lastToken, prefix, tokens, currentTokenIdx);
        } catch (e) {
          // eslint-disable-next-line no-console
          console.warn('Exception thrown in completer: ', e);
        }
        return [];
      })
      .reduce((acc, cur) => [...acc, ...cur], []);

    const uniqResults = uniqBy(sortBy(results, ['score', 'name']), 'name');
    callback(null, uniqResults);
  }
}
