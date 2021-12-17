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
import { sortBy, uniqBy } from 'lodash';

import type { TimeRange, NoTimeRangeOverride } from 'views/logic/queries/Query';

import type {
  Editor,
  ResultsCallback,
  Session,
  Position,
  CompletionResult,
  AutoCompleter,
  Token,
  Line,
} from './ace-types';

export interface Completer {
  getCompletions(
    currentToken: Token | undefined | null,
    lastToken: Token | undefined | null,
    prefix: string,
    tokens: Array<Token>,
    currentTokenIdx: number,
    timeRange?: TimeRange | NoTimeRangeOverride,
    streams?: Array<string>
  ): Array<CompletionResult> | Promise<Array<CompletionResult>>;
  shouldShowCompletions?: (currentLine: number, lines: Array<Array<Line>>) => boolean;
}

const onCompleterError = (error: Error) => {
  // eslint-disable-next-line no-console
  console.warn('Exception thrown in completer: ', error);
};

export default class SearchBarAutoCompletions implements AutoCompleter {
  completers: Array<Completer>;

  timeRange: TimeRange | NoTimeRangeOverride | undefined;

  streams: Array<string>;

  constructor(completers: Array<Completer> = [], timeRange?: TimeRange | NoTimeRangeOverride | undefined, streams?: Array<string>) {
    this.completers = completers;
    this.timeRange = timeRange;
    this.streams = streams;
  }

  getCompletions = async (editor: Editor, session: Session, pos: Position, prefix: string, callback: ResultsCallback) => {
    // eslint-disable-next-line no-param-reassign
    editor.completer.autoSelect = false;
    const tokens = editor.session.getTokens(pos.row);
    const currentToken = editor.session.getTokenAt(pos.row, pos.column);
    const currentTokenIdx = tokens.findIndex((t) => (t === currentToken));

    const lastToken: Token | undefined | null = currentTokenIdx > 0 ? tokens[currentTokenIdx - 1] : null;

    const results = await Promise.all(
      this.completers
        .map(async (completer) => {
          try {
            return await completer.getCompletions(currentToken, lastToken, prefix, tokens, currentTokenIdx, this.timeRange, this.streams);
          } catch (e) {
            onCompleterError(e);
          }

          return [];
        }),
    );
    const uniqResults = uniqBy(sortBy(results.flat(), ['score', 'name']), 'name');

    callback(null, uniqResults);
  }

  shouldShowCompletions = (currentLine: number, lines: Array<Array<Line>>) => {
    return this.completers.some((completer) => {
      if (typeof completer.shouldShowCompletions === 'function') {
        try {
          return completer.shouldShowCompletions(currentLine, lines);
        } catch (e) {
          onCompleterError(e);
        }
      }

      return false;
    });
  }
}
