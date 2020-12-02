declare module 'reflux' {
  import { RefluxActions, Store } from 'stores/StoreTypes';

  export interface StoreDefinition {
    listenables?: any[];
    init?: Function;
    getInitialState?: Function;

    [propertyName: string]: any;
  }

  export interface ActionsDefinition {
    [index: string]: { asyncResult: boolean};
  }

  type ElementType <T extends ReadonlyArray<unknown>> = T extends ReadonlyArray<infer E> ? E : never;

  export function createStore<T>(definition: StoreDefinition): Store<T> & typeof definition;
  export function createActions<R>(definitions: ActionsDefinition): RefluxActions<R>;
  export function createActions<R>(definitions: R): RefluxActions<{ [key in ElementType<typeof definitions>]: () => Promise<unknown> }>;
  export function connect(store: Store, key?: string): void;
}
