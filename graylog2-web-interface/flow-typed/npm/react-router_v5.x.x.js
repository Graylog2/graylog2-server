// flow-typed signature: 7a910df5fd688ad6d23c9ceaa43bf1ea
// flow-typed version: 5f4b3cb313/react-router_v5.x.x/flow_>=v0.104.x

declare module "react-router" {
  // NOTE: many of these are re-exported by react-router-dom and
  // react-router-native, so when making changes, please be sure to update those
  // as well.
  declare export type Location = $ReadOnly<{
    pathname: string,
    search: string,
    hash: string,
    state?: any,
    key?: string,
    ...
  }>;

  declare export type LocationShape = {
    pathname?: string,
    search?: string,
    hash?: string,
    state?: any,
    ...
  };

  declare export type HistoryAction = "PUSH" | "REPLACE" | "POP";

  declare export type RouterHistory = {
    length: number,
    location: Location,
    action: HistoryAction,
    listen(
      callback: (location: Location, action: HistoryAction) => void
    ): () => void,
    push(path: string | LocationShape, state?: any): void,
    replace(path: string | LocationShape, state?: any): void,
    go(n: number): void,
    goBack(): void,
    goForward(): void,
    canGo?: (n: number) => boolean,
    block(
      callback: string | (location: Location, action: HistoryAction) => ?string
    ): () => void,
    ...
  };

  declare export type Match = {
    params: { [key: string]: ?string, ... },
    isExact: boolean,
    path: string,
    url: string,
    ...
  };

  declare export type ContextRouter = {|
    history: RouterHistory,
    location: Location,
    match: Match,
    staticContext?: StaticRouterContext
  |};

  declare export type GetUserConfirmation = (
    message: string,
    callback: (confirmed: boolean) => void
  ) => void;

  declare type StaticRouterContext = { url?: string, ... };

  declare export class StaticRouter extends React$Component<{
    basename?: string,
    location?: string | Location,
    context: StaticRouterContext,
    children?: React$Node,
    ...
  }> {}

  declare export class MemoryRouter extends React$Component<{
    initialEntries?: Array<LocationShape | string>,
    initialIndex?: number,
    getUserConfirmation?: GetUserConfirmation,
    keyLength?: number,
    children?: React$Node,
    ...
  }> {}

  declare export class Router extends React$Component<{
    history: RouterHistory,
    children?: React$Node,
    ...
  }> {}

  declare export class Prompt extends React$Component<{
    message: string | ((location: Location) => string | true),
    when?: boolean,
    ...
  }> {}

  declare export class Redirect extends React$Component<{|
    to: string | LocationShape,
    push?: boolean,
    from?: string,
    exact?: boolean,
    strict?: boolean
  |}> {}


  declare export class Route extends React$Component<{|
    component?: React$ComponentType<*>,
    render?: (router: ContextRouter) => React$Node,
    children?: React$ComponentType<ContextRouter> | React$Node,
    path?: string | Array<string>,
    exact?: boolean,
    strict?: boolean,
    location?: LocationShape,
    sensitive?: boolean
  |}> {}

  declare export class Switch extends React$Component<{|
    children?: React$Node,
    location?: Location
  |}> {}

  declare export function withRouter<P>(
    Component: React$ComponentType<{| ...ContextRouter, ...P |}>
  ): React$ComponentType<P>;

  declare type MatchPathOptions = {
    path?: string | string[],
    exact?: boolean,
    strict?: boolean,
    sensitive?: boolean,
    ...
  };

  declare export function matchPath(
    pathname: string,
    options?: MatchPathOptions | string | string[]
  ): null | Match;

  declare export function useHistory(): $PropertyType<ContextRouter, 'history'>;
  declare export function useLocation(): $PropertyType<ContextRouter, 'location'>;
  declare export function useParams(): $PropertyType<$PropertyType<ContextRouter, 'match'>, 'params'>;
  declare export function useRouteMatch(path?: MatchPathOptions | string | string[]): $PropertyType<ContextRouter, 'match'>;

  declare export function generatePath(pattern?: string, params?: {...}): string;

  declare export default {
    StaticRouter: typeof StaticRouter,
    MemoryRouter: typeof MemoryRouter,
    Router: typeof Router,
    Prompt: typeof Prompt,
    Redirect: typeof Redirect,
    Route: typeof Route,
    Switch: typeof Switch,
    withRouter: typeof withRouter,
    matchPath: typeof matchPath,
    generatePath: typeof generatePath,
    ...
  };
}
