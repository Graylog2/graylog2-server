// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

// $FlowFixMe: imports from core need to be fixed in flow
import connect from 'stores/connect';
// $FlowFixMe: imports from core need to be fixed in flow
import Spinner from 'components/common/Spinner';

import { SearchExecutionStateStore } from 'views/stores/SearchExecutionStateStore';
import withPluginEntities from 'views/logic/withPluginEntities';
import type { ViewHook } from 'views/logic/hooks/ViewHook';
import type { ViewLoaderFn } from 'views/logic/views/ViewLoader';
import ViewLoader from 'views/logic/views/ViewLoader';

import ExtendedSearchPage from './ExtendedSearchPage';

type Props = {
  executingViewHooks: Array<ViewHook>,
  loadingViewHooks: Array<ViewHook>,
  location: {
    query: { [string]: any },
  },
  params: {
    viewId: string,
  },
  route: any,
  viewLoader: ViewLoaderFn,
};

type State = {
  hookComponent: ?any,
  loaded: boolean,
};

class ShowViewPage extends React.Component<Props, State> {
  static propTypes = {
    location: PropTypes.shape({
      query: PropTypes.object,
    }).isRequired,
    params: PropTypes.shape({
      viewId: PropTypes.string.isRequired,
    }).isRequired,
    route: PropTypes.object.isRequired,
    viewLoader: PropTypes.func,
  };

  static defaultProps = {
    viewLoader: ViewLoader,
  };

  state = {
    hookComponent: undefined,
    loaded: false,
  };

  componentDidMount = () => {
    const { location, params, loadingViewHooks, executingViewHooks, viewLoader } = this.props;
    const { viewId } = params;
    const { query } = location;
    return viewLoader(
      viewId,
      loadingViewHooks,
      executingViewHooks,
      query,
      () => {
        this.setState({ hookComponent: undefined });
      },
      (e) => {
        if (e instanceof Error) {
          throw e;
        }
        this.setState({ hookComponent: e });
      },
    )
      .then((results) => {
        this.setState({ loaded: true });
        return results;
      })
      .catch(e => e);
  };

  render() {
    const { hookComponent, loaded } = this.state;
    if (hookComponent) {
      const HookComponent = hookComponent;
      return <HookComponent />;
    }

    if (!loaded) {
      return <Spinner />;
    }

    const { route } = this.props;

    return (
      <ExtendedSearchPage route={route} />
    );
  }
}

const ShowViewPageWithStores = connect(ShowViewPage, { executionState: SearchExecutionStateStore });
const mapping = {
  loadingViewHooks: 'views.hooks.loadingView',
  executingViewHooks: 'views.hooks.executingView',
};
export default withPluginEntities(ShowViewPageWithStores, mapping);
