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

export type Token = {
  type: string,
  value: string,
};

export type Session = {
  getTokens: (no: number) => Array<Token>,
  getTokenAt: (no: number, idx: number) => Token | undefined | null,
  getValue: () => string,
};

export type Renderer = {
  scroller: HTMLElement,
  emptyMessageNode: HTMLElement | undefined | null,
};

export type Command = {
  name: string,
  bindKey: {
    win: string,
    mac: string,
  },
  // eslint-disable-next-line no-use-before-define
  exec: (editor: Editor) => void,
};

export type Commands = {
  addCommand: (command: Command) => void,
  removeCommands: (commands: Array<string>) => void,
};

export type Popup = {
  hide: () => void,
};

export type Completer = {
  autoSelect: boolean,
  popup: Popup,
};

export type Editor = {
  commands: Commands,
  completer: Completer,
  // eslint-disable-next-line no-use-before-define
  completers: Array<AutoCompleter>,
  session: Session,
  renderer: Renderer,
  setFontSize: (newFontSize: number) => void,
};

export type CompletionResult = {
  name: string,
  value: string,
  score: number,
  meta: any,
};

export type ResultsCallback = (obj: null, results: Array<CompletionResult>) => void;

export type Position = {
  row: number,
  column: number,
};

export interface AutoCompleter {
  getCompletions(editor: Editor, session: Session, position: Position, prefix: string, callback: ResultsCallback): void;
}
