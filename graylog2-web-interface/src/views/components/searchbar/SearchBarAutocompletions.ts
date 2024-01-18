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
import sortBy from 'lodash/sortBy';
import uniqBy from 'lodash/uniqBy';

import type { TimeRange, NoTimeRangeOverride } from 'views/logic/queries/Query';
import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import type View from 'views/logic/views/View';

import type {
  Editor,
  ResultsCallback,
  Session,
  Position,
  CompletionResult,
  AutoCompleter,
  Token,
  Line,
} from './queryinput/ace-types';

export type FieldTypes = { all: FieldIndex, query: FieldIndex };
type FieldIndex = { [fieldName: string]: FieldTypeMapping };

export type CompleterContext = Readonly<{
  currentToken: Token | undefined | null,
  prevToken: Token | undefined | null,
  prefix: string,
  tokens: Array<Token>,
  currentTokenIdx: number,
  timeRange?: TimeRange | NoTimeRangeOverride,
  streams?: Array<string>,
  fieldTypes?: FieldTypes,
  userTimezone: string,
  view: View,
}>;

export interface Completer {
  getCompletions(context: CompleterContext): Array<CompletionResult> | Promise<Array<CompletionResult>>;
  shouldShowCompletions?: (currentLine: number, lines: Array<Array<Line>>) => boolean;
  identifierRegexps?: RegExp[];
}

const onCompleterError = (error: Error) => {
  // eslint-disable-next-line no-console
  console.warn('Exception thrown in completer: ', error);
};

const getCurrentTokenIdx = (session: Session, pos: Position) => {
  let idx = 0;

  for (let rowIdx = 0; rowIdx <= pos.row; rowIdx += 1) {
    const row = session.getTokens(rowIdx);

    if (rowIdx === pos.row) {
      const currentToken = session.getTokenAt(pos.row, pos.column);
      const idxInActiveLine = row.findIndex((t) => (t === currentToken));
      idx += idxInActiveLine;
    } else {
      idx += row.length;
    }
  }

  return idx;
};

const formatTokens = (session: Session, pos: Position) => {
  const rowAmount = session.getLength();
  const allTokens = [...Array(rowAmount).keys()]
    .map((_, index) => session.getTokens(index))
    .filter((line) => !!line?.length)
    .flat();
  const currentTokenIdx = getCurrentTokenIdx(session, pos);
  const currentToken = allTokens[currentTokenIdx];
  const prevToken = allTokens[currentTokenIdx - 1] ?? null;

  return {
    tokens: allTokens,
    currentTokenIdx,
    currentToken,
    prevToken,
  };
};

export default class SearchBarAutoCompletions implements AutoCompleter {
  private readonly completers: Array<Completer>;

  private readonly timeRange: TimeRange | NoTimeRangeOverride | undefined;

  private readonly streams: Array<string>;

  private readonly fieldTypes: FieldTypes;

  private readonly userTimezone: string;

  private readonly view: View | undefined;

  constructor(
    completers: Array<Completer>,
    timeRange: TimeRange | NoTimeRangeOverride | undefined,
    streams: Array<string>,
    fieldTypes: FieldTypes,
    userTimezone: string,
    view?: View,
  ) {
    this.completers = completers;
    this.timeRange = timeRange;
    this.streams = streams;
    this.fieldTypes = fieldTypes;
    this.userTimezone = userTimezone;
    this.view = view;
  }

  getCompletions = async (editor: Editor, _session: Session, pos: Position, prefix: string, callback: ResultsCallback) => {
    const { tokens, currentToken, currentTokenIdx, prevToken } = formatTokens(editor.session, pos);

    const results = await Promise.all(
      this.completers
        .map(async (completer) => {
          try {
            return await completer.getCompletions({
              currentToken,
              prevToken,
              prefix,
              tokens,
              currentTokenIdx,
              timeRange: this.timeRange,
              streams: this.streams,
              fieldTypes: this.fieldTypes,
              userTimezone: this.userTimezone,
              view: this.view,
            });
          } catch (e) {
            onCompleterError(e);
          }

          return [];
        }),
    );
    const uniqResults = uniqBy(sortBy(results.flat(), ['score', 'name']), 'name');

    callback(null, uniqResults);
  };

  get identifierRegexps() { return this.completers.map((completer) => completer.identifierRegexps ?? []).flat(); }
}
