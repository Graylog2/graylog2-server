// @flow strict
import history from 'util/History';
import Routes from 'routing/Routes';

export const loadNewView = () => history.push(`${Routes.SEARCH}/new`);

export const loadNewViewForStream = (streamId: string) => history.push(`${Routes.stream_search(streamId)}/new`);

export const loadView = (viewId: string) => history.push(`${Routes.SEARCH}/${viewId}`);
