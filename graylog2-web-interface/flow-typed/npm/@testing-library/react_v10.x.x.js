// flow-typed signature: 2ec4c8cd8f67266b906a8137b83f9fc7
// flow-typed version: 11a906f160/@testing-library/react_v10.x.x/flow_>=v0.104.x

declare module '@testing-library/react' {
  // This type comes from
  // https://github.com/facebook/flow/blob/v0.104.0/lib/react-dom.js#L64
  declare type ReactDOMTestUtilsThenable = {
    then(resolve: () => mixed, reject?: () => mixed): mixed,
    ...
  };
  // This type comes from
  // https://github.com/facebook/flow/blob/v0.104.0/lib/react-dom.js#L116
  declare type ReactDOMTestUtilsAct = (
    callback: () => void | ReactDOMTestUtilsThenable
  ) => ReactDOMTestUtilsThenable;

  declare type WaitForElementOptions = {|
    container?: HTMLElement,
    timeout?: number,
    mutationObserverOptions?: MutationObserverInit,
  |};

  declare type TextMatch =
    | string
    | RegExp
    | ((content: string, element: HTMLElement) => boolean);

  declare type TextMatchOptions = {
    exact?: boolean,
    trim?: boolean,
    collapseWhitespace?: boolean,
    normalizer?: (text: string) => string,
    ...
  };

  declare type SelectorMatchOptions = {
    selector?: string,
    ...
  } & TextMatchOptions;

  declare type QueryByBoundAttribute = (
    text: TextMatch,
    options?: TextMatchOptions
  ) => ?HTMLElement;

  declare type AllByBoundAttribute = (
    text: TextMatch,
    options?: TextMatchOptions
  ) => Array<HTMLElement>;

  declare type FindAllByBoundAttribute = (
    text: TextMatch,
    options?: TextMatchOptions,
    waitForElementOptions?: WaitForElementOptions
  ) => Promise<HTMLElement[]>;

  declare type GetByBoundAttribute = (
    text: TextMatch,
    options?: TextMatchOptions
  ) => HTMLElement;

  declare type FindByBoundAttribute = (
    text: TextMatch,
    options?: TextMatchOptions,
    waitForElementOptions?: WaitForElementOptions
  ) => Promise<HTMLElement>;

  declare type QueryByText = (
    text: TextMatch,
    options?: SelectorMatchOptions
  ) => ?HTMLElement;

  declare type AllByText = (
    text: TextMatch,
    options?: SelectorMatchOptions
  ) => Array<HTMLElement>;

  declare type FindAllByText = (
    text: TextMatch,
    options?: SelectorMatchOptions,
    waitForElementOptions?: WaitForElementOptions
  ) => Promise<HTMLElement[]>;

  declare type GetByText = (
    text: TextMatch,
    options?: SelectorMatchOptions
  ) => HTMLElement;

  declare type FindByText = (
    text: TextMatch,
    options?: SelectorMatchOptions,
    waitForElementOptions?: WaitForElementOptions
  ) => Promise<HTMLElement>;

  declare type ByRoleOptions = {
    /**
     * If true includes elements in the query set that are usually excluded from
     * the accessibility tree. `role="none"` or `role="presentation"` are included
     * in either case.
     * @default false
     */
    hidden?: boolean,
    /**
     * Includes every role used in the `role` attribute
     * For example *ByRole('progressbar', {queryFallbacks: true})` will find <div role="meter progresbar">`.
     */
    queryFallbacks?: boolean,
    /**
     * Only considers  elements with the specified accessible name.
     */
    name?:
      | string
      | RegExp
      | ((accessibleName: string, element: Element) => boolean),
    ...
  } & TextMatchOptions;

  declare type AllByRole = (
    role: TextMatch,
    options?: ByRoleOptions
  ) => HTMLElement[];

  declare type GetByRole = (
    role: TextMatch,
    options?: ByRoleOptions
  ) => HTMLElement;

  declare type QueryByRole = (
    role: TextMatch,
    options?: ByRoleOptions
  ) => HTMLElement | null;

  declare type FindByRole = (
    role: TextMatch,
    options?: ByRoleOptions,
    waitForElementOptions?: WaitForElementOptions
  ) => Promise<HTMLElement>;

  declare type FindAllByRole = (
    role: Matcher,
    options?: ByRoleOptions,
    waitForElementOptions?: WaitForElementOptions
  ) => Promise<HTMLElement[]>;

  declare type GetsAndQueries = {|
    getByLabelText: GetByText,
    getAllByLabelText: AllByText,
    queryByLabelText: QueryByText,
    queryAllByLabelText: AllByText,
    findByLabelText: FindByText,
    findAllByLabelText: FindAllByText,

    getByPlaceholderText: GetByBoundAttribute,
    getAllByPlaceholderText: AllByBoundAttribute,
    queryByPlaceholderText: QueryByBoundAttribute,
    queryAllByPlaceholderText: AllByBoundAttribute,
    findByPlaceholderText: FindByBoundAttribute,
    findAllByPlaceholderText: FindAllByBoundAttribute,

    getByText: GetByText,
    getAllByText: AllByText,
    queryByText: QueryByText,
    queryAllByText: AllByText,
    findByText: FindByText,
    findAllByText: FindAllByText,

    getByAltText: GetByBoundAttribute,
    getAllByAltText: AllByBoundAttribute,
    queryByAltText: QueryByBoundAttribute,
    queryAllByAltText: AllByBoundAttribute,
    findByAltText: FindByBoundAttribute,
    findAllByAltText: FindAllByBoundAttribute,

    getByTitle: GetByBoundAttribute,
    getAllByTitle: AllByBoundAttribute,
    queryByTitle: QueryByBoundAttribute,
    queryAllByTitle: AllByBoundAttribute,
    findByTitle: FindByBoundAttribute,
    findAllByTitle: FindAllByBoundAttribute,

    getByDisplayValue: GetByBoundAttribute,
    getAllByDisplayValue: AllByBoundAttribute,
    queryByDisplayValue: QueryByBoundAttribute,
    queryAllByDisplayValue: AllByBoundAttribute,
    findByDisplayValue: FindByBoundAttribute,
    findAllByDisplayValue: FindAllByBoundAttribute,

    getByRole: GetByRole,
    getAllByRole: AllByRole,
    queryByRole: QueryByRole,
    queryAllByRole: AllByRole,
    findByRole: FindByRole,
    findAllByRole: FindAllByRole,

    getByTestId: GetByBoundAttribute,
    getAllByTestId: AllByBoundAttribute,
    queryByTestId: QueryByBoundAttribute,
    queryAllByTestId: AllByBoundAttribute,
    findByTestId: FindByBoundAttribute,
    findAllByTestId: FindAllByBoundAttribute,
  |};

  declare type FireEvent<TInit> = (
    element: HTMLElement,
    eventProperties?: TInit
  ) => boolean;

  declare type Screen<Queries = GetsAndQueries> = {
    ...Queries,
    debug: (
      baseElement?:
        | HTMLElement
        | DocumentFragment
        | Array<HTMLElement | DocumentFragment>,
      maxLength?: number
    ) => void,
    ...
  };

  declare type RenderResult<Queries = GetsAndQueries> = {
    ...Queries,
    container: HTMLElement,
    unmount: () => boolean,
    baseElement: HTMLElement,
    asFragment: () => DocumentFragment,
    debug: (
      baseElement?:
        | HTMLElement
        | DocumentFragment
        | Array<HTMLElement | DocumentFragment>,
      maxLength?: number
    ) => void,
    rerender: (ui: React$Element<*>) => void,
    ...
  };

  declare export type RenderOptionsWithoutCustomQueries = {|
    container?: HTMLElement,
    baseElement?: HTMLElement,
    hydrate?: boolean,
    wrapper?: React.ComponentType,
  |};

  declare export type RenderOptionsWithCustomQueries<
    CustomQueries: { ... }
  > = {|
    queries: CustomQueries,
    container?: HTMLElement,
    baseElement?: HTMLElement,
    hydrate?: boolean,
    wrapper?: React.ComponentType,
  |};

  declare export function render(
    ui: React.ReactElement<any>,
    options?: RenderOptionsWithoutCustomQueries
  ): RenderResult<>;
  declare export function render<
    CustomQueries: { [string]: (...args: Array<any>) => any, ... }
  >(
    ui: React.ReactElement<any>,
    options: RenderOptionsWithCustomQueries<CustomQueries>
  ): RenderResult<CustomQueries>;

  declare export var act: ReactDOMTestUtilsAct;
  declare export function cleanup(): Promise<void>;

  declare export function waitFor<T>(
    callback: () => T,
    options?: {|
      container?: HTMLElement,
      timeout?: number,
      interval?: number,
      mutationObserverOptions?: MutationObserverInit,
    |}
  ): Promise<T>;

  declare export function waitForElementToBeRemoved<T>(
    callback: (() => T) | T,
    options?: {|
      container?: HTMLElement,
      timeout?: number,
      interval?: number,
      mutationObserverOptions?: MutationObserverInit,
    |}
  ): Promise<T>;

  /* Deprecated */
  declare export function wait(
    callback?: () => void,
    options?: {
      timeout?: number,
      interval?: number,
      ...
    }
  ): Promise<void>;

  /* Deprecated */
  declare export function waitForDomChange<T>(options?: {
    container?: HTMLElement,
    timeout?: number,
    mutationObserverOptions?: MutationObserverInit,
    ...
  }): Promise<T>;

  /* Deprecated */
  declare export function waitForElement<T>(
    callback?: () => T,
    options?: {
      container?: HTMLElement,
      timeout?: number,
      mutationObserverOptions?: MutationObserverInit,
      ...
    }
  ): Promise<T>;

  declare export function within(
    element: HTMLElement,
    queriesToBind?: GetsAndQueries | Array<GetsAndQueries>
  ): GetsAndQueries;

  declare export var fireEvent: {|
    (element: HTMLElement, event: Event): void,

    copy: FireEvent<Event$Init>,
    cut: FireEvent<Event$Init>,
    paste: FireEvent<Event$Init>,
    compositionEnd: FireEvent<Event$Init>,
    compositionStart: FireEvent<Event$Init>,
    compositionUpdate: FireEvent<Event$Init>,
    keyDown: FireEvent<Event$Init>,
    keyPress: FireEvent<Event$Init>,
    keyUp: FireEvent<Event$Init>,
    focus: FireEvent<Event$Init>,
    blur: FireEvent<Event$Init>,
    change: FireEvent<Event$Init>,
    input: FireEvent<Event$Init>,
    invalid: FireEvent<Event$Init>,
    submit: FireEvent<Event$Init>,
    click: FireEvent<MouseEvent$MouseEventInit>,
    contextMenu: FireEvent<MouseEvent$MouseEventInit>,
    dblClick: FireEvent<MouseEvent$MouseEventInit>,
    doubleClick: FireEvent<MouseEvent$MouseEventInit>,
    drag: FireEvent<MouseEvent$MouseEventInit>,
    dragEnd: FireEvent<MouseEvent$MouseEventInit>,
    dragEnter: FireEvent<MouseEvent$MouseEventInit>,
    dragExit: FireEvent<MouseEvent$MouseEventInit>,
    dragLeave: FireEvent<MouseEvent$MouseEventInit>,
    dragOver: FireEvent<MouseEvent$MouseEventInit>,
    dragStart: FireEvent<MouseEvent$MouseEventInit>,
    drop: FireEvent<MouseEvent$MouseEventInit>,
    mouseDown: FireEvent<MouseEvent$MouseEventInit>,
    mouseEnter: FireEvent<MouseEvent$MouseEventInit>,
    mouseLeave: FireEvent<MouseEvent$MouseEventInit>,
    mouseMove: FireEvent<MouseEvent$MouseEventInit>,
    mouseOut: FireEvent<MouseEvent$MouseEventInit>,
    mouseOver: FireEvent<MouseEvent$MouseEventInit>,
    mouseUp: FireEvent<MouseEvent$MouseEventInit>,
    select: FireEvent<Event$Init>,
    touchCancel: FireEvent<Event$Init>,
    touchEnd: FireEvent<Event$Init>,
    touchMove: FireEvent<Event$Init>,
    touchStart: FireEvent<Event$Init>,
    scroll: FireEvent<Event$Init>,
    wheel: FireEvent<MouseEvent$MouseEventInit>,
    abort: FireEvent<Event$Init>,
    canPlay: FireEvent<Event$Init>,
    canPlayThrough: FireEvent<Event$Init>,
    durationChange: FireEvent<Event$Init>,
    emptied: FireEvent<Event$Init>,
    encrypted: FireEvent<Event$Init>,
    ended: FireEvent<Event$Init>,
    loadedData: FireEvent<Event$Init>,
    loadedMetadata: FireEvent<Event$Init>,
    loadStart: FireEvent<Event$Init>,
    pause: FireEvent<Event$Init>,
    play: FireEvent<Event$Init>,
    playing: FireEvent<Event$Init>,
    progress: FireEvent<Event$Init>,
    rateChange: FireEvent<Event$Init>,
    seeked: FireEvent<Event$Init>,
    seeking: FireEvent<Event$Init>,
    stalled: FireEvent<Event$Init>,
    suspend: FireEvent<Event$Init>,
    timeUpdate: FireEvent<Event$Init>,
    volumeChange: FireEvent<Event$Init>,
    waiting: FireEvent<Event$Init>,
    load: FireEvent<Event$Init>,
    error: FireEvent<Event$Init>,
    animationStart: FireEvent<Event$Init>,
    animationEnd: FireEvent<Event$Init>,
    animationIteration: FireEvent<Event$Init>,
    transitionEnd: FireEvent<Event$Init>,
  |};
  // dom-testing-library re-declares
  declare export function queryByTestId(
    container: HTMLElement,
    id: TextMatch,
    options?: TextMatchOptions
  ): ?HTMLElement;
  declare export function getByTestId(
    container: HTMLElement,
    id: TextMatch,
    options?: TextMatchOptions
  ): HTMLElement;
  declare export function queryByText(
    container: HTMLElement,
    text: TextMatch,
    options?: TextMatchOptions
  ): ?HTMLElement;
  declare export function getByText(
    container: HTMLElement,
    text: TextMatch,
    options?: { selector?: string, ... } & TextMatchOptions
  ): HTMLElement;
  declare export function queryByPlaceholderText(
    container: HTMLElement,
    text: TextMatch,
    options?: TextMatchOptions
  ): ?HTMLElement;
  declare export function getByPlaceholderText(
    container: HTMLElement,
    text: TextMatch,
    options?: TextMatchOptions
  ): HTMLElement;
  declare export function queryByLabelText(
    container: HTMLElement,
    text: TextMatch,
    options?: TextMatchOptions
  ): ?HTMLElement;
  declare export function getByLabelText(
    container: HTMLElement,
    text: TextMatch,
    options?: { selector?: string, ... } & TextMatchOptions
  ): HTMLElement;
  declare export function queryByAltText(
    container: HTMLElement,
    text: TextMatch,
    options?: TextMatchOptions
  ): ?HTMLElement;
  declare export function getByAltText(
    container: HTMLElement,
    text: TextMatch,
    options?: TextMatchOptions
  ): HTMLElement;
  declare export function getNodeText(
    node: HTMLElement,
  ): string;
  declare export var screen: Screen<>;
}
