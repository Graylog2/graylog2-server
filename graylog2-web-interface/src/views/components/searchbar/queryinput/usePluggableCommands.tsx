import { useMemo } from 'react';

import type { Usage, CustomCommandContext } from 'views/components/searchbar/queryinput/types';
import type { Command, Editor } from 'views/components/searchbar/queryinput/ace-types';
import usePluginEntities from 'hooks/usePluginEntities';

const useCommandsContext = (usage: Usage): CustomCommandContext => {
  const contextProviders = usePluginEntities('views.queryInput.commandContextProviders');

  const context = useMemo(() => Object.fromEntries(contextProviders.map(({ key, provider }) => [key, provider()])), [contextProviders]);

  return { ...context, usage };
};

const usePluggableCommands = (usage: Usage): Array<Command> => {
  const pluggableCommands = usePluginEntities('views.queryInput.commands');
  const commandsForUsage = useMemo(() => pluggableCommands.filter(({ usages = [] }) => usages.includes(usage)), [pluggableCommands, usage]);
  const context = useCommandsContext(usage);

  return useMemo(() => commandsForUsage.map(({ name, bindKey, exec: commandExec }) => ({
    name,
    bindKey,
    exec: (editor: Editor) => commandExec(editor, context),
  })), [commandsForUsage, context]);
};

export default usePluggableCommands;
