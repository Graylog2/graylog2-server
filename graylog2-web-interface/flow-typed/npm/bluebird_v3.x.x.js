// flow-typed signature: b9b7f7cbfe41f4e7d48a790df32071b7
// flow-typed version: e529cd1be1/bluebird_v3.x.x/flow_>=v0.70.x

type Bluebird$RangeError = Error;
type Bluebird$CancellationErrors = Error;
type Bluebird$TimeoutError = Error;
type Bluebird$RejectionError = Error;
type Bluebird$OperationalError = Error;

type Bluebird$ConcurrencyOption = {
  concurrency: number
};
type Bluebird$SpreadOption = {
  spread: boolean
};
type Bluebird$MultiArgsOption = {
  multiArgs: boolean
};
type Bluebird$BluebirdConfig = {
  warnings?: boolean,
  longStackTraces?: boolean,
  cancellation?: boolean,
  monitoring?: boolean
};

declare class Bluebird$PromiseInspection<T> {
  isCancelled(): boolean;
  isFulfilled(): boolean;
  isRejected(): boolean;
  pending(): boolean;
  reason(): any;
  value(): T;
}

type Bluebird$PromisifyOptions = {|
  multiArgs?: boolean,
  context: any
|};

declare type Bluebird$PromisifyAllOptions = {
  suffix?: string,
  filter?: (
    name: string,
    func: Function,
    target?: any,
    passesDefaultFilter?: boolean
  ) => boolean,
  // The promisifier gets a reference to the original method and should return a function which returns a promise
  promisifier?: (originalMethod: Function) => () => Bluebird$Promise<any>
};

declare type $Promisable<T> = Promise<T> | T;

declare class Bluebird$Disposable<R> {}

declare class Bluebird$Promise<+R> extends Promise<R> {
  static RangeError: Class<Bluebird$RangeError>;
  static CancellationErrors: Class<Bluebird$CancellationErrors>;
  static TimeoutError: Class<Bluebird$TimeoutError>;
  static RejectionError: Class<Bluebird$RejectionError>;
  static OperationalError: Class<Bluebird$OperationalError>;
  
  static Defer: Class<Bluebird$Defer>;
  static PromiseInspection: Class<Bluebird$PromiseInspection<*>>;

  static all<T>(
    Promises: $Promisable<Iterable<$Promisable<T>>>
  ): Bluebird$Promise<Array<T>>;
  static props(
    input: Object | Map<*, *> | $Promisable<Object | Map<*, *>>
  ): Bluebird$Promise<*>;
  static any<T, Elem: $Promisable<T>>(
    Promises: Iterable<Elem> | $Promisable<Iterable<Elem>>
  ): Bluebird$Promise<T>;
  static race<T, Elem: $Promisable<T>>(
    Promises: Iterable<Elem> | $Promisable<Iterable<Elem>>
  ): Bluebird$Promise<T>;
  static reject<T>(error?: any): Bluebird$Promise<T>;
  static resolve<T>(object?: $Promisable<T>): Bluebird$Promise<T>;
  static some<T, Elem: $Promisable<T>>(
    Promises: Iterable<Elem> | $Promisable<Iterable<Elem>>,
    count: number
  ): Bluebird$Promise<Array<T>>;
  static join<T, A>(
    value1: $Promisable<A>,
    handler: (a: A) => $Promisable<T>
  ): Bluebird$Promise<T>;
  static join<T, A, B>(
    value1: $Promisable<A>,
    value2: $Promisable<B>,
    handler: (a: A, b: B) => $Promisable<T>
  ): Bluebird$Promise<T>;
  static join<T, A, B, C>(
    value1: $Promisable<A>,
    value2: $Promisable<B>,
    value3: $Promisable<C>,
    handler: (a: A, b: B, c: C) => $Promisable<T>
  ): Bluebird$Promise<T>;
  static map<T, U, Elem: $Promisable<T>>(
    Promises: Iterable<Elem> | $Promisable<Iterable<Elem>>,
    mapper: (item: T, index: number, arrayLength: number) => $Promisable<U>,
    options?: Bluebird$ConcurrencyOption
  ): Bluebird$Promise<Array<U>>;
  static mapSeries<T, U, Elem: $Promisable<T>>(
    Promises: Iterable<Elem> | $Promisable<Iterable<Elem>>,
    mapper: (item: T, index: number, arrayLength: number) => $Promisable<U>
  ): Bluebird$Promise<Array<U>>;
  static reduce<T, U, Elem: $Promisable<T>>(
    Promises: Iterable<Elem> | $Promisable<Iterable<Elem>>,
    reducer: (
      total: U,
      current: T,
      index: number,
      arrayLength: number
    ) => $Promisable<U>,
    initialValue?: $Promisable<U>
  ): Bluebird$Promise<U>;
  static filter<T, Elem: $Promisable<T>>(
    Promises: Iterable<Elem> | $Promisable<Iterable<Elem>>,
    filterer: (
      item: T,
      index: number,
      arrayLength: number
    ) => $Promisable<boolean>,
    option?: Bluebird$ConcurrencyOption
  ): Bluebird$Promise<Array<T>>;
  static each<T, Elem: $Promisable<T>>(
    Promises: Iterable<Elem> | $Promisable<Iterable<Elem>>,
    iterator: (
      item: T,
      index: number,
      arrayLength: number
    ) => $Promisable<mixed>
  ): Bluebird$Promise<Array<T>>;
  static try<T>(
    fn: () => $Promisable<T>,
    args: ?Array<any>,
    ctx: ?any
  ): Bluebird$Promise<T>;
  static attempt<T>(
    fn: () => $Promisable<T>,
    args: ?Array<any>,
    ctx: ?any
  ): Bluebird$Promise<T>;
  static delay<T>(ms: number, value: $Promisable<T>): Bluebird$Promise<T>;
  static delay(ms: number): Bluebird$Promise<void>;
  static config(config: Bluebird$BluebirdConfig): void;

  static defer(): Bluebird$Defer;
  static setScheduler(
    scheduler: (callback: (...args: Array<any>) => void) => void
  ): void;
  static promisify(
    nodeFunction: Function,
    receiver?: Bluebird$PromisifyOptions
  ): Function;
  static promisifyAll(
    target: Object | Array<Object>,
    options?: Bluebird$PromisifyAllOptions
  ): void;

  static coroutine(generatorFunction: Function): Function;
  static spawn<T>(generatorFunction: Function): Promise<T>;

  // It doesn't seem possible to have type-generics for a variable number of arguments.
  // Handle up to 3 arguments, then just give up and accept 'any'.
  static method<T, R: $Promisable<T>>(fn: () => R): () => Bluebird$Promise<T>;
  static method<T, R: $Promisable<T>, A>(
    fn: (a: A) => R
  ): (a: A) => Bluebird$Promise<T>;
  static method<T, R: $Promisable<T>, A, B>(
    fn: (a: A, b: B) => R
  ): (a: A, b: B) => Bluebird$Promise<T>;
  static method<T, R: $Promisable<T>, A, B, C>(
    fn: (a: A, b: B, c: C) => R
  ): (a: A, b: B, c: C) => Bluebird$Promise<T>;
  static method<T, R: $Promisable<T>>(
    fn: (...args: any) => R
  ): (...args: any) => Bluebird$Promise<T>;

  static cast<T>(value: $Promisable<T>): Bluebird$Promise<T>;
  // static bind(ctx: any): Bluebird$Promise<void>;
  static is(value: any): boolean;
  static longStackTraces(): void;

  static onPossiblyUnhandledRejection(handler: (reason: any) => any): void;
  static fromCallback<T>(
    resolver: (fn: (error: ?Error, value?: T) => any) => any,
    options?: Bluebird$MultiArgsOption
  ): Bluebird$Promise<T>;

  constructor(
    callback: (
      resolve: (result?: $Promisable<R>) => void,
      reject: (error?: any) => void
    ) => mixed
  ): void;
  then(onFulfill: null | void, onReject: null | void): Bluebird$Promise<R>;
  then<U>(
    onFulfill: null | void,
    onReject: (error: any) => Promise<U> | U
  ): Bluebird$Promise<R | U>;
  then<U>(
    onFulfill: (value: R) => Promise<U> | U,
    onReject: null | void | ((error: any) => Promise<U> | U)
  ): Bluebird$Promise<U>;
  catch(onReject: null | void): Promise<R>;
  catch<U>(onReject?: (error: any) => $Promisable<U>): Bluebird$Promise<U>;
  catch<U, ErrorT: Error>(
    err: Class<ErrorT>,
    onReject: (error: ErrorT) => $Promisable<U>
  ): Bluebird$Promise<U>;
  catch<U, ErrorT: Error>(
    err1: Class<ErrorT>,
    err2: Class<ErrorT>,
    onReject: (error: ErrorT) => $Promisable<U>
  ): Bluebird$Promise<U>;
  catch<U, ErrorT: Error>(
    err1: Class<ErrorT>,
    err2: Class<ErrorT>,
    err3: Class<ErrorT>,
    onReject: (error: ErrorT) => $Promisable<U>
  ): Bluebird$Promise<U>;
  caught<U, ErrorT: Error>(
    err: Class<ErrorT>,
    onReject: (error: Error) => $Promisable<U>
  ): Bluebird$Promise<U>;
  caught<U, ErrorT: Error>(
    err1: Class<ErrorT>,
    err2: Class<ErrorT>,
    onReject: (error: ErrorT) => $Promisable<U>
  ): Bluebird$Promise<U>;
  caught<U, ErrorT: Error>(
    err1: Class<ErrorT>,
    err2: Class<ErrorT>,
    err3: Class<ErrorT>,
    onReject: (error: ErrorT) => $Promisable<U>
  ): Bluebird$Promise<U>;
  caught<U>(onReject: (error: any) => $Promisable<U>): Bluebird$Promise<U>;

  error<U>(onReject?: (error: any) => ?$Promisable<U>): Bluebird$Promise<U>;
  done<U>(
    onFulfill?: (value: R) => mixed,
    onReject?: (error: any) => mixed
  ): void;
  finally<T>(onDone?: (value: R) => mixed): Bluebird$Promise<T>;
  lastly<T>(onDone?: (value: R) => mixed): Bluebird$Promise<T>;
  tap<T>(onDone?: (value: R) => mixed): Bluebird$Promise<T>;
  delay(ms: number): Bluebird$Promise<R>;
  timeout(ms: number, message?: string): Bluebird$Promise<R>;
  cancel(): void;

  // bind(ctx: any): Bluebird$Promise<R>;
  call(propertyName: string, ...args: Array<any>): Bluebird$Promise<any>;
  throw(reason: Error): Bluebird$Promise<R>;
  thenThrow(reason: Error): Bluebird$Promise<R>;
  all<T>(): Bluebird$Promise<Array<T>>;
  any<T>(): Bluebird$Promise<T>;
  some<T>(count: number): Bluebird$Promise<Array<T>>;
  race<T>(): Bluebird$Promise<T>;
  map<T, U>(
    mapper: (item: T, index: number, arrayLength: number) => $Promisable<U>,
    options?: Bluebird$ConcurrencyOption
  ): Bluebird$Promise<Array<U>>;
  mapSeries<T, U>(
    mapper: (item: T, index: number, arrayLength: number) => $Promisable<U>
  ): Bluebird$Promise<Array<U>>;
  reduce<T, U>(
    reducer: (
      total: T,
      item: U,
      index: number,
      arrayLength: number
    ) => $Promisable<T>,
    initialValue?: $Promisable<T>
  ): Bluebird$Promise<T>;
  filter<T>(
    filterer: (
      item: T,
      index: number,
      arrayLength: number
    ) => $Promisable<boolean>,
    options?: Bluebird$ConcurrencyOption
  ): Bluebird$Promise<Array<T>>;
  each<T, U>(
    iterator: (item: T, index: number, arrayLength: number) => $Promisable<U>
  ): Bluebird$Promise<Array<T>>;
  asCallback<T>(
    callback: (error: ?any, value?: T) => any,
    options?: Bluebird$SpreadOption
  ): void;
  return<T>(value: T): Bluebird$Promise<T>;
  thenReturn<T>(value: T): Bluebird$Promise<T>;
  spread<T>(...args: Array<T>): Bluebird$Promise<*>;

  reflect(): Bluebird$Promise<Bluebird$PromiseInspection<*>>;

  isFulfilled(): boolean;
  isRejected(): boolean;
  isPending(): boolean;
  isResolved(): boolean;

  value(): R;
  reason(): any;

  disposer(
    disposer: (value: R, promise: Promise<*>) => void
  ): Bluebird$Disposable<R>;

  static using<T, A>(
    disposable: Bluebird$Disposable<T>,
    handler: (value: T) => $Promisable<A>
  ): Bluebird$Promise<A>;
  
  suppressUnhandledRejections(): void;
}

declare class Bluebird$Defer {
  promise: Bluebird$Promise<*>;
  resolve: (value: any) => any;
  reject: (value: any) => any;
}

declare module "bluebird" {
  declare module.exports: typeof Bluebird$Promise;

  declare type Disposable<T> = Bluebird$Disposable<T>;
}
