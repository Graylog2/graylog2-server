import { $Call } from 'utility-types';

type ExtractResultType<R> = (arg0: (...args: any[]) => Promise<R>) => R;

export type ListenableAction<R> = R & {
  $call: R;
  listen: (arg0: (arg0: $Call<ExtractResultType<R>>) => any) => () => void;
  completed: {
    listen: (arg0: (arg0: $Call<ExtractResultType<R>>) => any) => () => void;
  };
  promise: (arg0: Promise<$Call<ExtractResultType<R>>>) => void;
};

export type RefluxActions<A> = { [P in keyof A]: ListenableAction<A[P]> };

export type Store<State> = {
  listen: (arg0: (arg0: State) => unknown) => () => void;
  getInitialState: () => State;
};
