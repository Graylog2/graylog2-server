// @flow strict
import View from 'enterprise/logic/views/View';

export type ViewHookArguments = {
  view: View,
  retry: () => Promise<*>,
  query: { [string]: any },
};

export type ViewHook = (ViewHookArguments) => Promise<boolean>;
