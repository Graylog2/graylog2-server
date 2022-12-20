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

import usePluggableCommands from 'views/components/searchbar/queryinput/usePluggableCommands';
import type { Command } from 'views/components/searchbar/queryinput/ace-types';
import type { Usage } from 'views/components/searchbar/queryinput/types';

type Props = {
  children: (commands: Array<Command>) => React.ReactElement,
  usage: Usage,
}

const PluggableCommands = ({ children, usage }: Props) => {
  const customCommands = usePluggableCommands(usage);

  return useMemo(() => children(customCommands), [children, customCommands]);
};

export default PluggableCommands;
