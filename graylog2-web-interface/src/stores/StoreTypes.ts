import { $Call } from 'utility-types';

type ExtractResultType<R> = $Call<ExtractTypeFromPromise<ReturnType<R>>>;
type ExtractTypeFromPromise<R> = (promise: Promise<R>) => R;

export type ListenableAction<R extends (...args: any[]) => Promise<any>> = R & {
  listen: (cb: (result: ExtractResultType<R>) => any) => () => void;
  completed: {
    listen: (cb: (result: ExtractResultType<R>) => any) => () => void;
  };
  promise: (promise: ReturnType<R>) => void;
};

export type RefluxActions<A> = { [P in keyof A]: ListenableAction<A[P]> };

export type Store<State> = {
  listen: (cb: (state: State) => unknown) => () => void;
  getInitialState: () => State;
};
