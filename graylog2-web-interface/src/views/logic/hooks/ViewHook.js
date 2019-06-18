// @flow strict
import View from 'views/logic/views/View';

export type ViewHookArguments = {
  view: View,
  retry: () => Promise<*>,
  query: { [string]: any },
};

export type ViewHook = (ViewHookArguments) => Promise<boolean>;
