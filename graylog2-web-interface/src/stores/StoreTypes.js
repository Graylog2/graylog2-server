// @flow strict

type ExtractResultType = <R>((...any) => Promise<R>) => R;

export type ListenableAction<R> = R & {
  $call: R,
  listen: (($Call<ExtractResultType, R>) => *) => () => void,
  completed: {
    listen: (($Call<ExtractResultType, R>) => *) => () => void,
  },
  promise: (Promise<$Call<ExtractResultType, R>>) => void,
};
export type RefluxAction = <Fn>(Fn) => ListenableAction<Fn>;

export type RefluxActions<A> = $ObjMap<A, RefluxAction>;

export type Store<State> = {
  listen: ((State) => mixed) => () => void,
  getInitialState: () => State,
};
