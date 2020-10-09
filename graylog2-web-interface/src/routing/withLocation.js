// @flow strict
import * as React from 'react';
import { withRouter } from 'react-router';

export type Location = {
  query: { [string]: ?string },
  pathname: string,
  search: string,
};

type LocationContext = {
  location: Location,
};

const withLocation = <Props: {...}, ComponentType: React$ComponentType<Props>>(Component: ComponentType): React$ComponentType<$Diff<React$ElementConfig<ComponentType>, LocationContext>> => withRouter(Component);

export default withLocation;
