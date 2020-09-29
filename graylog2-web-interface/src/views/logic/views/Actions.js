// @flow strict
import history from 'util/History';

const loadNewView = () => {
  return history.push('/search');
};

const loadView = (viewId: string) => {
  return history.push(`/search/${viewId}`);
};

export {
  loadNewView,
  loadView,
};
