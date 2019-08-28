// @flow strict

// eslint-disable-next-line no-undef
type ExtractResultType = <R>((...any) => Promise<R>) => R;

export type ListenableAction<R> = R & {
  $call: R,
  // eslint-disable-next-line no-undef
  listen: (($Call<ExtractResultType, R>) => *) => () => void,
  completed: {
    // eslint-disable-next-line no-undef
    listen: (($Call<ExtractResultType, R>) => *) => () => void,
  },
  // eslint-disable-next-line no-undef
  promise: (Promise<$Call<ExtractResultType, R>>) => void,
};
// eslint-disable-next-line no-undef
export type RefluxAction = <Fn>(Fn) => ListenableAction<Fn>;

// eslint-disable-next-line no-undef
export type RefluxActions<A> = $ObjMap<A, RefluxAction>;
