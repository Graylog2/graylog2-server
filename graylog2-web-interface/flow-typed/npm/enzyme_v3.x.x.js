// flow-typed signature: 20e2bb4e722c2a79e4e0f178491bf6cc
// flow-typed version: 1f669c8dd2/enzyme_v3.x.x/flow_>=v0.104.x

declare module "enzyme" {
  declare type PredicateFunction<T: Wrapper<*>> = (
    wrapper: T,
    index: number
  ) => boolean;
  declare type UntypedSelector = string | { [key: string]: number|string|boolean, ... };
  declare type EnzymeSelector = UntypedSelector | React$ElementType;

  // CheerioWrapper is a type alias for an actual cheerio instance
  // TODO: Reference correct type from cheerio's type declarations
  declare type CheerioWrapper = any;

  declare class Wrapper<RootComponent> {
    at(index: number): this,
    childAt(index: number): this,
    children(selector?: UntypedSelector): this,
    children<T: React$ElementType>(selector: T): ReactWrapper<T>,
    closest(selector: UntypedSelector): this,
    closest<T: React$ElementType>(selector: T): ReactWrapper<T>,
    contains(nodes: React$Element<any> | $ReadOnlyArray<React$Element<any>>): boolean,
    containsAllMatchingElements(nodes: $ReadOnlyArray<React$Element<any>>): boolean,
    containsAnyMatchingElements(nodes: $ReadOnlyArray<React$Element<any>>): boolean,
    containsMatchingElement(node: React$Element<any>): boolean,
    context(key?: string): any,
    debug(options?: Object): string,
    dive(option?: { context?: Object, ... }): this,
    equals(node: React$Element<any>): boolean,
    every(selector: EnzymeSelector): boolean,
    everyWhere(predicate: PredicateFunction<this>): boolean,
    exists(selector?: EnzymeSelector): boolean,
    filter(selector: UntypedSelector): this,
    filter<T: React$ElementType>(selector: T): ReactWrapper<T>,
    filterWhere(predicate: PredicateFunction<this>): this,
    find(selector: UntypedSelector): this,
    find<T: React$ElementType>(selector: T): ReactWrapper<T>,
    findWhere(predicate: PredicateFunction<this>): this,
    first(): this,
    forEach(fn: (node: this, index: number) => mixed): this,
    get<T = any>(index: number): React$Element<T>,
    getDOMNode(): HTMLElement | HTMLInputElement,
    hasClass(className: string): boolean,
    hostNodes(): this,
    html(): string,
    instance(): React$ElementRef<RootComponent>,
    invoke(propName: string): (...args: $ReadOnlyArray<any>) => mixed,
    is(selector: EnzymeSelector): boolean,
    isEmpty(): boolean,
    isEmptyRender(): boolean,
    key(): string,
    last(): this,
    length: number,
    map<T>(fn: (node: this, index: number) => T): Array<T>,
    matchesElement(node: React$Element<any>): boolean,
    name(): string,
    not(selector: EnzymeSelector): this,
    parent(): this,
    parents(selector?: UntypedSelector): this,
    parents<T: React$ElementType>(selector: T): ReactWrapper<T>,
    prop(key: string): any,
    props(): Object,
    reduce<T>(
      fn: (value: T, node: this, index: number) => T,
      initialValue?: T
    ): Array<T>,
    reduceRight<T>(
      fn: (value: T, node: this, index: number) => T,
      initialValue?: T
    ): Array<T>,
    render(): CheerioWrapper,
    renderProp(propName: string): (...args: Array<any>) => this,
    setContext(context: Object): this,
    setProps(props: {...}, callback?: () => void): this,
    setState(state: {...}, callback?: () => void): this,
    simulate(event: string, ...args: Array<any>): this,
    simulateError(error: Error): this,
    slice(begin?: number, end?: number): this,
    some(selector: EnzymeSelector): boolean,
    someWhere(predicate: PredicateFunction<this>): boolean,
    state(key?: string): any,
    text(): string,
    type(): string | Function | null,
    unmount(): this,
    update(): this,
  }

  declare class ReactWrapper<T> extends Wrapper<T> {
    constructor(nodes: React$Element<T>, root: any, options?: ?Object): ReactWrapper<T>,
    mount(): this,
    ref(refName: string): this,
    detach(): void
  }

  declare class ShallowWrapper<T> extends Wrapper<T> {
    constructor(
      nodes: React$Element<T>,
      root: any,
      options?: ?Object
    ): ShallowWrapper<T>,
    shallow(options?: { context?: Object, ... }): ShallowWrapper<T>,
    getElement<T = any>(): React$Element<T>,
    getElements<T = any>(): Array<React$Element<T>>
  }

  declare function shallow<T>(
    node: React$Element<T>,
    options?: {
      context?: Object,
      disableLifecycleMethods?: boolean,
      ...
    }
  ): ShallowWrapper<T>;

  declare function mount<T>(
    node: React$Element<T>,
    options?: {
      context?: Object,
      attachTo?: HTMLElement,
      childContextTypes?: Object,
      ...
    }
  ): ReactWrapper<T>;

  declare function render(
    node: React$Node,
    options?: { context?: Object, ... }
  ): CheerioWrapper;

  declare module.exports: {
    configure(options: {
      Adapter?: any,
      disableLifecycleMethods?: boolean,
      ...
    }): void,
    render: typeof render,
    mount: typeof mount,
    shallow: typeof shallow,
    ShallowWrapper: typeof ShallowWrapper,
    ReactWrapper: typeof ReactWrapper,
    ...
  };
}
