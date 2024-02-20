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

type EventName = 'tokenizerUpdate';

type Tokenizer = {
  bgTokenizer: {
    currentLine: number,
    lines: Array<Array<Line>>,
  }
};
type EventCallback = {
  tokenizerUpdate: (input: string, tokenizer: Tokenizer) => void,
};

export type Session = {
  curOp: { args: unknown },
  getLength: () => number,
  getTokens: (no: number) => Array<Token>,
  getTokenAt: (no: number, idx: number) => Token | undefined | null,
  getValue: () => string,
  on: <T extends EventName>(event: T, cb: EventCallback[T]) => void,
  bgTokenizer: { lines: Array<Array<Line>> },
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

  exec: (editor: Editor) => void,
};

export type Commands = {
  addCommand: (command: Command) => void,
  removeCommands: (commands: Array<string>) => void,
  on: (commandName: string, callback: () => void) => void
};

export type Popup = {
  hide: () => void,
};

export type Completer = {
  autoSelect: boolean,
  popup?: Popup,
  activated: boolean,
  insertMatch: () => boolean,
  detach: () => void,
  goTo: (direction: string) => void,
  keyboardHandler: {
    commandKeyBinding: {
      tab: Command,
    },
    addCommand: (command: Command) => void
  }
};

export type Editor = {
  container: HTMLElement | undefined,
  commands: Commands,
  completer: Completer,
  completers: Array<AutoCompleter>,
  execCommand: (command: string, args?: Record<string, unknown>) => void,
  focus: () => void,
  session: Session,
  renderer: Renderer,
  setFontSize: (newFontSize: number) => void,
  getValue: () => string,
  tabstopManager: unknown,
  setValue: (newValue: string) => void,
  isFocused: () => boolean,
};

export type CompletionResult = {
  name?: string,
  value: string,
  score: number,
  meta?: any,
  caption?: string,
};

export type ResultsCallback = (obj: null, results: Array<CompletionResult>) => void;

export type Position = {
  row: number,
  column: number,
};

export type Line = {
  type: string,
  value: string,
  index?: number,
  start?: number,
}

export interface AutoCompleter {
  getCompletions(
    editor: Editor,
    session: Session,
    position: Position,
    prefix: string,
    callback: ResultsCallback
  ): void;
}
