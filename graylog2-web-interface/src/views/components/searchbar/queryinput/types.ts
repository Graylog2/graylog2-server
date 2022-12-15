import type { Editor } from 'views/components/searchbar/queryinput/ace-types';

export type Usage = 'search_query' | 'widget_query' | 'global_override_query';

export interface CustomCommandContext {
  usage: Usage;
}

export type CustomCommandExec = (editor: Editor, context: CustomCommandContext) => void;

export type CustomCommand = {
  usages: Array<Usage>,
  name: string,
  bindKey: {
    mac: string,
    win: string,
  },
  exec: CustomCommandExec,
}
