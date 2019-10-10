// flow-typed signature: 59dd3f43a52a4247c89dc056aedeb359
// flow-typed version: c6154227d1/styled-components_v4.x.x/flow_>=v0.92.0 <=v0.103.x

// @flow

declare module 'styled-components' {
  declare type BuiltinElementInstances = {
    a: React$ElementRef<'a'>,
    abbr: React$ElementRef<'abbr'>,
    address: React$ElementRef<'address'>,
    area: React$ElementRef<'area'>,
    article: React$ElementRef<'article'>,
    aside: React$ElementRef<'aside'>,
    audio: React$ElementRef<'audio'>,
    b: React$ElementRef<'b'>,
    base: React$ElementRef<'base'>,
    bdi: React$ElementRef<'bdi'>,
    bdo: React$ElementRef<'bdo'>,
    big: React$ElementRef<'big'>,
    blockquote: React$ElementRef<'blockquote'>,
    body: React$ElementRef<'body'>,
    br: React$ElementRef<'br'>,
    button: React$ElementRef<'button'>,
    canvas: React$ElementRef<'canvas'>,
    caption: React$ElementRef<'caption'>,
    cite: React$ElementRef<'cite'>,
    code: React$ElementRef<'code'>,
    col: React$ElementRef<'col'>,
    colgroup: React$ElementRef<'colgroup'>,
    data: React$ElementRef<'data'>,
    datalist: React$ElementRef<'datalist'>,
    dd: React$ElementRef<'dd'>,
    del: React$ElementRef<'del'>,
    details: React$ElementRef<'details'>,
    dfn: React$ElementRef<'dfn'>,
    dialog: React$ElementRef<'dialog'>,
    div: React$ElementRef<'div'>,
    dl: React$ElementRef<'dl'>,
    dt: React$ElementRef<'dt'>,
    em: React$ElementRef<'em'>,
    embed: React$ElementRef<'embed'>,
    fieldset: React$ElementRef<'fieldset'>,
    figcaption: React$ElementRef<'figcaption'>,
    figure: React$ElementRef<'figure'>,
    footer: React$ElementRef<'footer'>,
    form: React$ElementRef<'form'>,
    h1: React$ElementRef<'h1'>,
    h2: React$ElementRef<'h2'>,
    h3: React$ElementRef<'h3'>,
    h4: React$ElementRef<'h4'>,
    h5: React$ElementRef<'h5'>,
    h6: React$ElementRef<'h6'>,
    head: React$ElementRef<'head'>,
    header: React$ElementRef<'header'>,
    hgroup: React$ElementRef<'hgroup'>,
    hr: React$ElementRef<'hr'>,
    html: React$ElementRef<'html'>,
    i: React$ElementRef<'i'>,
    iframe: React$ElementRef<'iframe'>,
    img: React$ElementRef<'img'>,
    input: React$ElementRef<'input'>,
    ins: React$ElementRef<'ins'>,
    kbd: React$ElementRef<'kbd'>,
    label: React$ElementRef<'label'>,
    legend: React$ElementRef<'legend'>,
    li: React$ElementRef<'li'>,
    link: React$ElementRef<'link'>,
    main: React$ElementRef<'main'>,
    map: React$ElementRef<'map'>,
    mark: React$ElementRef<'mark'>,
    menu: React$ElementRef<'menu'>,
    meta: React$ElementRef<'meta'>,
    meter: React$ElementRef<'meter'>,
    nav: React$ElementRef<'nav'>,
    noscript: React$ElementRef<'noscript'>,
    object: React$ElementRef<'object'>,
    ol: React$ElementRef<'ol'>,
    optgroup: React$ElementRef<'optgroup'>,
    option: React$ElementRef<'option'>,
    output: React$ElementRef<'output'>,
    p: React$ElementRef<'p'>,
    param: React$ElementRef<'param'>,
    picture: React$ElementRef<'picture'>,
    pre: React$ElementRef<'pre'>,
    progress: React$ElementRef<'progress'>,
    q: React$ElementRef<'q'>,
    rp: React$ElementRef<'rp'>,
    rt: React$ElementRef<'rt'>,
    ruby: React$ElementRef<'ruby'>,
    s: React$ElementRef<'s'>,
    samp: React$ElementRef<'samp'>,
    script: React$ElementRef<'script'>,
    section: React$ElementRef<'section'>,
    select: React$ElementRef<'select'>,
    small: React$ElementRef<'small'>,
    source: React$ElementRef<'source'>,
    span: React$ElementRef<'span'>,
    strong: React$ElementRef<'strong'>,
    style: React$ElementRef<'style'>,
    sub: React$ElementRef<'sub'>,
    summary: React$ElementRef<'summary'>,
    sup: React$ElementRef<'sup'>,
    table: React$ElementRef<'table'>,
    tbody: React$ElementRef<'tbody'>,
    td: React$ElementRef<'td'>,
    textarea: React$ElementRef<'textarea'>,
    tfoot: React$ElementRef<'tfoot'>,
    th: React$ElementRef<'th'>,
    thead: React$ElementRef<'thead'>,
    time: React$ElementRef<'time'>,
    title: React$ElementRef<'title'>,
    tr: React$ElementRef<'tr'>,
    track: React$ElementRef<'track'>,
    u: React$ElementRef<'u'>,
    ul: React$ElementRef<'ul'>,
    var: React$ElementRef<'var'>,
    video: React$ElementRef<'video'>,
    wbr: React$ElementRef<'wbr'>,

    // SVG
    circle: React$ElementRef<'circle'>,
    clipPath: React$ElementRef<'clipPath'>,
    defs: React$ElementRef<'defs'>,
    ellipse: React$ElementRef<'ellipse'>,
    g: React$ElementRef<'g'>,
    image: React$ElementRef<'image'>,
    line: React$ElementRef<'line'>,
    linearGradient: React$ElementRef<'linearGradient'>,
    mask: React$ElementRef<'mask'>,
    path: React$ElementRef<'path'>,
    pattern: React$ElementRef<'pattern'>,
    polygon: React$ElementRef<'polygon'>,
    polyline: React$ElementRef<'polyline'>,
    radialGradient: React$ElementRef<'radialGradient'>,
    rect: React$ElementRef<'rect'>,
    stop: React$ElementRef<'stop'>,
    svg: React$ElementRef<'svg'>,
    text: React$ElementRef<'text'>,
    tspan: React$ElementRef<'tspan'>,

    // Deprecated, should be HTMLUnknownElement, but Flow doesn't support it
    keygen: React$ElementRef<'keygen'>,
    menuitem: React$ElementRef<'menuitem'>,
  }

  declare type BuiltinElementType<ElementName: string> = $ElementType<BuiltinElementInstances, ElementName>

  declare class InterpolatableComponent<P> extends React$Component<P> {
    static +styledComponentId: string;
  }

  declare export type Interpolation<P> =
                                       | ((executionContext: P) => Interpolation<any>) // eslint-disable-line flowtype/no-weak-types
                                       | InterpolatableComponent<any> // eslint-disable-line flowtype/no-weak-types
                                       | CSSRules
                                       | KeyFrames
                                       | string
                                       | number


  // Should this be `mixed` perhaps?
  declare export type CSSRules = Interpolation<any>[] // eslint-disable-line flowtype/no-weak-types

  // This is not exported on purpose, since it's an implementation detail
  declare type TaggedTemplateLiteral<I, R> = (strings: string[], ...interpolations: Interpolation<I>[]) => R

  declare export type CSSConstructor = TaggedTemplateLiteral<any, CSSRules> // eslint-disable-line flowtype/no-weak-types
  declare export type KeyFramesConstructor = TaggedTemplateLiteral<any, KeyFrames> // eslint-disable-line flowtype/no-weak-types
  declare export type CreateGlobalStyleConstructor = TaggedTemplateLiteral<any, React$ComponentType<*>> // eslint-disable-line flowtype/no-weak-types

  declare interface Tag<T> {
    styleTag: HTMLStyleElement | null;
    getIds(): string[];
    hasNameForId(id: string, name: string): boolean;
    insertMarker(id: string): T;
    insertRules(id: string, cssRules: string[], name: ?string): void;
    removeRules(id: string): void;
    css(): string;
    toHTML(additionalAttrs: ?string): string;
    toElement(): React$Element<*>;
    clone(): Tag<T>;
    sealed: boolean;
  }

  // The `any`/weak types in here all come from `styled-components` directly, since those definitions were just copied over
  declare export class StyleSheet {
    static get master() : StyleSheet;
    static get instance() : StyleSheet;
    static reset(forceServer? : boolean) : void;

    id : number;
    forceServer : boolean;
    target : ?HTMLElement;
    tagMap : {[string]: Tag<any>}; // eslint-disable-line flowtype/no-weak-types
    deferred: { [string]: string[] | void };
    rehydratedNames: { [string]: boolean };
    ignoreRehydratedNames: { [string]: boolean };
    tags: Tag<any>[]; // eslint-disable-line flowtype/no-weak-types
    importRuleTag: Tag<any>; // eslint-disable-line flowtype/no-weak-types
    capacity: number;
    clones: StyleSheet[];

    constructor(?HTMLElement) : this;
    rehydrate() : this;
    clone() : StyleSheet;
    sealAllTags() : void;
    makeTag(tag : ?Tag<any>) : Tag<any>; // eslint-disable-line flowtype/no-weak-types
    getImportRuleTag() : Tag<any>; // eslint-disable-line flowtype/no-weak-types
    getTagForId(id : string): Tag<any>; // eslint-disable-line flowtype/no-weak-types
    hasId(id: string) : boolean;
    hasNameForId(id: string, name: string) : boolean;
    deferredInject(id : string, cssRules : string[]) : void;
    inject(id: string, cssRules : string[], name? : string) : void;
    remove(id : string) : void;
    toHtml() : string;
    toReactElements() : React$ElementType[];
  }

  declare export class KeyFrames {
    id : string;
    name : string;
    rules : string[];

    constructor(name : string, rules : string[]) : this;
    inject(StyleSheet) : void;
    toString() : string;
    getName() : string;
  }

  // I think any is appropriate here?
  // eslint-disable-next-line flowtype/no-weak-types
  declare export var css : CSSConstructor;
  declare export var keyframes : KeyFramesConstructor;
  declare export var createGlobalStyle : CreateGlobalStyleConstructor
  declare export var ThemeProvider: React$ComponentType<{children?: ?React$Node, theme: mixed | (mixed) => mixed}>

  declare type ThemeProps<T> = {|
    theme: T
  |}

  declare type PropsWithTheme<Props, T> = {|
    ...ThemeProps<T>,
    ...$Exact<Props>
  |}

  declare export function withTheme<Theme, Config: {}, Instance>(Component: React$AbstractComponent<Config, Instance>): React$AbstractComponent<$Diff<Config, ThemeProps<Theme | void>>, Instance>

  declare export type StyledComponent<Props, Theme, Instance> = React$AbstractComponent<Props, Instance> & InterpolatableComponent<Props>
  declare interface StyledFactory<StyleProps, Props, Theme, Instance> {
    [[call]]: TaggedTemplateLiteral<PropsWithTheme<StyleProps, Theme>, StyledComponent<StyleProps, Theme, Instance>>;
    +attrs: <A: {}>(A | (StyleProps) => A) => React$AbstractComponent<$Diff<StyleProps, A>, Instance>;
  }


  declare type ConvenientShorthands = $ObjMap<
    BuiltinElementInstances,
    <V>(V) =>
      // TODO: Figure out how to add support for `attrs` API here
      // TODO: Would be nice if we could find a way to use `StyledFactory` here, but I am too dumb to figure it out.
      <StyleProps, Theme>(string[], ...Interpolation<PropsWithTheme<StyleProps, Theme>>[]) => StyledComponent<StyleProps, Theme, V>
   >

  declare interface Styled {
    <StyleProps, Theme, ElementName: $Keys<BuiltinElementInstances>>(ElementName): StyledFactory<StyleProps, {}, Theme, BuiltinElementType<ElementName>>;
    <StyleProps, Theme, OwnProps: {}, Comp: React$ComponentType<OwnProps>>(Comp): StyledFactory<StyleProps, OwnProps, Theme, React$ComponentType<Comp>>;
  }

  declare export default Styled & ConvenientShorthands
}



declare module 'styled-components/native' {

  declare class InterpolatableComponent<P> extends React$Component<P> {
    static +styledComponentId: string;
  }

  declare export type Interpolation<P> =
                                       | ((executionContext: P) => Interpolation<any>) // eslint-disable-line flowtype/no-weak-types
                                       | InterpolatableComponent<any> // eslint-disable-line flowtype/no-weak-types
                                       | CSSRules
                                       | KeyFrames
                                       | string
                                       | number
  // Should this be `mixed` perhaps?
  declare export type CSSRules = Interpolation<any>[] // eslint-disable-line flowtype/no-weak-types

  // This is not exported on purpose, since it's an implementation detail
  declare type TaggedTemplateLiteral<I, R> = (strings: string[], ...interpolations: Interpolation<I>[]) => R

  declare export type CSSConstructor = TaggedTemplateLiteral<any, CSSRules> // eslint-disable-line flowtype/no-weak-types
  declare export type KeyFramesConstructor = TaggedTemplateLiteral<any, KeyFrames> // eslint-disable-line flowtype/no-weak-types
  declare export type CreateGlobalStyleConstructor = TaggedTemplateLiteral<any, React$ComponentType<*>> // eslint-disable-line flowtype/no-weak-types

  declare interface Tag<T> {
    styleTag: HTMLStyleElement | null;
    getIds(): string[];
    hasNameForId(id: string, name: string): boolean;
    insertMarker(id: string): T;
    insertRules(id: string, cssRules: string[], name: ?string): void;
    removeRules(id: string): void;
    css(): string;
    toHTML(additionalAttrs: ?string): string;
    toElement(): React$Element<*>;
    clone(): Tag<T>;
    sealed: boolean;
  }

  // The `any`/weak types in here all come from `styled-components` directly, since those definitions were just copied over
  declare export class StyleSheet {
    static get master() : StyleSheet;
    static get instance() : StyleSheet;
    static reset(forceServer? : boolean) : void;

    id : number;
    forceServer : boolean;
    target : ?HTMLElement;
    tagMap : {[string]: Tag<any>}; // eslint-disable-line flowtype/no-weak-types
    deferred: { [string]: string[] | void };
    rehydratedNames: { [string]: boolean };
    ignoreRehydratedNames: { [string]: boolean };
    tags: Tag<any>[]; // eslint-disable-line flowtype/no-weak-types
    importRuleTag: Tag<any>; // eslint-disable-line flowtype/no-weak-types
    capacity: number;
    clones: StyleSheet[];

    constructor(?HTMLElement) : this;
    rehydrate() : this;
    clone() : StyleSheet;
    sealAllTags() : void;
    makeTag(tag : ?Tag<any>) : Tag<any>; // eslint-disable-line flowtype/no-weak-types
    getImportRuleTag() : Tag<any>; // eslint-disable-line flowtype/no-weak-types
    getTagForId(id : string): Tag<any>; // eslint-disable-line flowtype/no-weak-types
    hasId(id: string) : boolean;
    hasNameForId(id: string, name: string) : boolean;
    deferredInject(id : string, cssRules : string[]) : void;
    inject(id: string, cssRules : string[], name? : string) : void;
    remove(id : string) : void;
    toHtml() : string;
    toReactElements() : React$ElementType[];
  }

  declare export class KeyFrames {
    id : string;
    name : string;
    rules : string[];

    constructor(name : string, rules : string[]) : this;
    inject(StyleSheet) : void;
    toString() : string;
    getName() : string;
  }

  // I think any is appropriate here?
  // eslint-disable-next-line flowtype/no-weak-types
  declare export var css : CSSConstructor;
  declare export var keyframes : KeyFramesConstructor;
  declare export var createGlobalStyle : CreateGlobalStyleConstructor
  declare export var ThemeProvider: React$ComponentType<{children?: ?React$Node, theme: mixed | (mixed) => mixed}>

  declare type ThemeProps<T> = {|
    theme: T
  |}

  declare type PropsWithTheme<Props, T> = {|
    ...ThemeProps<T>,
    ...$Exact<Props>
  |}

  declare export function withTheme<Theme, Config: {}, Instance>(Component: React$AbstractComponent<Config, Instance>): React$AbstractComponent<$Diff<Config, ThemeProps<Theme | void>>, Instance>

  declare export type StyledComponent<Props, Theme, Instance> = React$AbstractComponent<Props, Instance> & InterpolatableComponent<Props>
  declare interface StyledFactory<StyleProps, Props, Theme, Instance> {
    [[call]]: TaggedTemplateLiteral<PropsWithTheme<StyleProps, Theme>, StyledComponent<StyleProps, Theme, Instance>>;
    +attrs: <A: {}>(A | (StyleProps) => A) => React$AbstractComponent<$Diff<StyleProps, A>, Instance>;
  }

  declare type BuiltinElementInstances = {
    ActivityIndicator:             React$ComponentType<{}>,
    ActivityIndicatorIOS:          React$ComponentType<{}>,
    ART:                           React$ComponentType<{}>,
    Button:                        React$ComponentType<{}>,
    DatePickerIOS:                 React$ComponentType<{}>,
    DrawerLayoutAndroid:           React$ComponentType<{}>,
    Image:                         React$ComponentType<{}>,
    ImageBackground:               React$ComponentType<{}>,
    ImageEditor:                   React$ComponentType<{}>,
    ImageStore:                    React$ComponentType<{}>,
    KeyboardAvoidingView:          React$ComponentType<{}>,
    ListView:                      React$ComponentType<{}>,
    MapView:                       React$ComponentType<{}>,
    Modal:                         React$ComponentType<{}>,
    NavigatorIOS:                  React$ComponentType<{}>,
    Picker:                        React$ComponentType<{}>,
    PickerIOS:                     React$ComponentType<{}>,
    ProgressBarAndroid:            React$ComponentType<{}>,
    ProgressViewIOS:               React$ComponentType<{}>,
    ScrollView:                    React$ComponentType<{}>,
    SegmentedControlIOS:           React$ComponentType<{}>,
    Slider:                        React$ComponentType<{}>,
    SliderIOS:                     React$ComponentType<{}>,
    SnapshotViewIOS:               React$ComponentType<{}>,
    Switch:                        React$ComponentType<{}>,
    RecyclerViewBackedScrollView:  React$ComponentType<{}>,
    RefreshControl:                React$ComponentType<{}>,
    SafeAreaView:                  React$ComponentType<{}>,
    StatusBar:                     React$ComponentType<{}>,
    SwipeableListView:             React$ComponentType<{}>,
    SwitchAndroid:                 React$ComponentType<{}>,
    SwitchIOS:                     React$ComponentType<{}>,
    TabBarIOS:                     React$ComponentType<{}>,
    Text:                          React$ComponentType<{}>,
    TextInput:                     React$ComponentType<{}>,
    ToastAndroid:                  React$ComponentType<{}>,
    ToolbarAndroid:                React$ComponentType<{}>,
    Touchable:                     React$ComponentType<{}>,
    TouchableHighlight:            React$ComponentType<{}>,
    TouchableNativeFeedback:       React$ComponentType<{}>,
    TouchableOpacity:              React$ComponentType<{}>,
    TouchableWithoutFeedback:      React$ComponentType<{}>,
    View:                          React$ComponentType<{}>,
    ViewPagerAndroid:              React$ComponentType<{}>,
    WebView:                       React$ComponentType<{}>,
    FlatList:                      React$ComponentType<{}>,
    SectionList:                   React$ComponentType<{}>,
    VirtualizedList:               React$ComponentType<{}>,
  }

  declare type BuiltinElementType<ElementName: string> = $ElementType<BuiltinElementInstances, ElementName>

  declare type ConvenientShorthands = $ObjMap<
    BuiltinElementInstances,
    <V>(V) =>
      // TODO: Figure out how to add support for `attrs` API here
      // TODO: Would be nice if we could find a way to use `StyledFactory` here, but I am too dumb to figure it out.
      <StyleProps, Theme>(string[], ...Interpolation<PropsWithTheme<StyleProps, Theme>>[]) => StyledComponent<StyleProps, Theme, V>
   >

  declare interface Styled {
    <StyleProps, Theme, ElementName: $Keys<BuiltinElementInstances>>(ElementName): StyledFactory<StyleProps, {}, Theme, BuiltinElementType<ElementName>>;
    <StyleProps, Theme, OwnProps: {}, Comp: React$ComponentType<OwnProps>>(Comp): StyledFactory<StyleProps, OwnProps, Theme, React$ComponentType<Comp>>;
  }

  declare export default Styled & ConvenientShorthands
}
