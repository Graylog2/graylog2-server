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
import { useMemo } from 'react';

import type { Usage, CustomCommandContext } from 'views/components/searchbar/queryinput/types';
import type { Command, Editor } from 'views/components/searchbar/queryinput/ace-types';
import usePluginEntities from 'hooks/usePluginEntities';

const useCommandsContext = (usage: Usage): CustomCommandContext => {
  const contextProviders = usePluginEntities('views.queryInput.commandContextProviders');

  const context = Object.fromEntries(contextProviders.map(({ key, provider }) => [key, provider()]));

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
