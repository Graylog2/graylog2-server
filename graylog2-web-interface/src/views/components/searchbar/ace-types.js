// @flow strict

export type Token = {
  type: string,
  value: string,
};

export type Session = {
  getTokens: (number) => Array<Token>,
  getTokenAt: (number, number) => ?Token,
  getValue: () => string,
};

export type Renderer = {
  scroller: HTMLElement,
  emptyMessageNode: ?HTMLElement,
};

export type Command = {
  name: string,
  bindKey: {
    win: string,
    mac: string,
  },
  exec: Editor => void,
};

export type Commands = {
  addCommand: Command => void,
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
  setFontSize: (number) => void,
};

export type CompletionResult = {
  name: string,
  value: string,
  score: number,
  meta: any,
};

export type ResultsCallback = (null, Array<CompletionResult>) => void;

export interface AutoCompleter {
  getCompletions(editor: Editor, session: Session, position: Position, prefix: string, callback: ResultsCallback): void;
}

export type Position = {
  row: number,
  column: number,
};
