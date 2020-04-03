// @flow strict
import * as React from 'react';

export type ActionContexts = { [string]: * };

const ActionContext = React.createContext<ActionContexts>({});

type Props = {
  children: React.Node,
  value: ActionContexts;
};

const AdditionalContext = {
  Provider: ({ children, value }: Props) => (
    <ActionContext.Consumer>
      {(contexts) => (
        <ActionContext.Provider value={{ ...contexts, ...value }}>
          {children}
        </ActionContext.Provider>
      )}
    </ActionContext.Consumer>
  ),
  Consumer: ActionContext.Consumer,
};

export { ActionContext, AdditionalContext };
