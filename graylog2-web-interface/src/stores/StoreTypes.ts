import { $Call } from 'utility-types';

type PromiseProvider = (...args: any[]) => Promise<any>;
type ExtractResultType<R extends PromiseProvider> = $Call<ExtractTypeFromPromise<ReturnType<R>>>;
type ExtractTypeFromPromise<R> = (promise: Promise<R>) => R;

export type ListenableAction<R extends PromiseProvider> = R & {
  listen: (cb: (result: ExtractResultType<R>) => any) => () => void;
  completed: {
    listen: (cb: (result: ExtractResultType<R>) => any) => () => void;
  };
  promise: (promise: ReturnType<R>) => void;
};

export type RefluxActions<A extends { [key: string]: PromiseProvider }> = { [P in keyof A]: ListenableAction<A[P]> };

export type Store<State> = {
  listen: (cb: (state: State) => unknown) => () => void;
  getInitialState: () => State;
};
