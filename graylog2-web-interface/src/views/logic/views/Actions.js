// @flow strict
import history from 'util/History';
import Routes from 'routing/Routes';

export const loadNewView = () => {
  return history.push(`${Routes.SEARCH}/new`);
};

export const loadNewViewForStream = (streamId: string) => {
  return history.push(`${Routes.stream_search(streamId)}/new`);
};

export const loadView = (viewId: string) => {
  return history.push(`${Routes.SEARCH}/${viewId}`);
};
