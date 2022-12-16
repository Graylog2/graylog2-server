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
