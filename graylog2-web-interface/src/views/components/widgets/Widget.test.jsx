// @flow strict
import React from 'react';
import renderer from 'react-test-renderer';
import { mount } from 'enzyme';
import { render, wait, fireEvent, cleanup } from '@testing-library/react';
import { browserHistory } from 'react-router';
import { Map } from 'immutable';
import Routes from 'routing/Routes';

import mockComponent from 'helpers/mocking/MockComponent';
import mockAction from 'helpers/mocking/MockAction';
import { WidgetActions } from 'views/stores/WidgetStore';
import { TitlesActions, TitleTypes } from 'views/stores/TitlesStore';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import WidgetModel from 'views/logic/widgets/Widget';
import View from 'views/logic/views/View';
import { DashboardsStore } from 'views/stores/DashboardsStore';
import { ViewStore } from 'views/stores/ViewStore';
import type { ViewStoreState } from 'views/stores/ViewStore';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import SearchActions from 'views/actions/SearchActions';
import Search from 'views/logic/search/Search';
import Query from 'views/logic/queries/Query';
import CopyWidgetToDashboard from 'views/logic/views/CopyWidgetToDashboard';
import asMock from 'helpers/mocking/AsMock';
import ViewState from 'views/logic/views/ViewState';

import Widget from './Widget';
import LoadingWidget from './LoadingWidget';
import ErrorWidget from './ErrorWidget';

jest.mock('views/actions/SearchActions', () => ({
  create: mockAction(jest.fn()),
  get: mockAction(jest.fn()),
}));

jest.mock('views/stores/ViewManagementStore', () => ({
  ViewManagementActions: {
    update: mockAction(jest.fn()),
    get: mockAction(jest.fn()),
  },
}));

jest.mock('views/logic/views/CopyWidgetToDashboard', () => jest.fn());

jest.mock('../searchbar/QueryInput', () => mockComponent('QueryInput'));
jest.mock('./WidgetHeader', () => 'widget-header');
jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: {
    exports: key => (key !== 'enterpriseWidgets' ? [] : [
      {
        type: 'dummy',
        displayName: 'Some Dummy Visualization',
        visualizationComponent: 'dummy-visualization',
        editComponent: 'edit-dummy-visualization',
      },
    ]),
  },
}));

jest.mock('views/stores/ChartColorRulesStore', () => ({
  ChartColorRulesStore: {},
}));
jest.mock('views/stores/WidgetStore');
jest.mock('views/stores/TitlesStore');
jest.mock('./WidgetColorContext', () => ({ children }) => children);

const createNodeMock = (element) => {
  if (element.type === 'div') {
    return {
      clientHeight: 420,
      clientWidth: 420,
    };
  }
  return null;
};

describe('<Widget />', () => {
  const widget = { config: {}, id: 'widgetId', type: 'dummy' };

  const viewState = ViewState.builder().build();
  const query = Query.builder().id('query-id').build();
  const searchSearch = Search.builder().queries([query]).id('search-1').build();
  const search = View.builder()
    .search(searchSearch)
    .type(View.Type.Dashboard)
    .state(Map.of('query-id', viewState))
    .id('search-1')
    .title('search 1')
    .build();
  const viewStoreState: ViewStoreState = {
    activeQuery: 'query-id',
    view: search,
    isNew: false,
    dirty: false,
  };

  const searchDB1 = Search.builder().id('search-1').build();
  const dashboard1 = View.builder().type(View.Type.Dashboard).id('view-1').title('view 1')
    .search(searchDB1)
    .build();
  const dashboard2 = View.builder().type(View.Type.Dashboard).id('view-2').title('view 2')
    .build();
  const dashboardList = [dashboard1, dashboard2];
  const dashboardState = {
    list: dashboardList,
    pagination: {
      total: 2,
      page: 1,
      per_page: 10,
      count: 2,
    },
  };


  const DummyWidget = props => (
    <Widget widget={widget}
            id="widgetId"
            fields={[]}
            onPositionsChange={() => {}}
            onSizeChange={() => {}}
            title="Widget Title"
            position={new WidgetPosition(1, 1, 1, 1)}
            {...props} />

  );
  it('should render with empty props', () => {
    const options = { createNodeMock };
    const wrapper = renderer.create(<DummyWidget />, options);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });
  it('should render loading widget for widget without data', () => {
    const wrapper = mount(<DummyWidget />);
    expect(wrapper.find(LoadingWidget)).toHaveLength(1);
  });
  it('should render error widget for widget with one error', () => {
    const wrapper = mount(<DummyWidget errors={[{ description: 'The widget has failed: the dungeon collapsed, you die!' }]} />);
    const errorWidgets = wrapper.find(ErrorWidget);
    expect(errorWidgets).toHaveLength(1);
    expect(errorWidgets).toIncludeText('The widget has failed: the dungeon collapsed, you die!');
  });
  it('should render error widget including all error messages for widget with multiple errors', () => {
    const wrapper = mount((
      <DummyWidget errors={[
        { description: 'Something is wrong' },
        { description: 'Very wrong' },
      ]} />
    ));
    const errorWidgets = wrapper.find(ErrorWidget);
    expect(errorWidgets).toHaveLength(1);
    expect(errorWidgets).toIncludeText('Something is wrong');
    expect(errorWidgets).toIncludeText('Very wrong');
  });
  it('should render correct widget visualization for widget with data', () => {
    const wrapper = mount(<DummyWidget data={[]} />);
    expect(wrapper.find(LoadingWidget)).toHaveLength(0);
    expect(wrapper.find('dummy-visualization')).toHaveLength(1);
  });

  it('copies title when duplicating widget', (done) => {
    const wrapper = mount(<DummyWidget title="Dummy Widget" />);

    wrapper.find('ActionToggle').simulate('click');
    const duplicate = wrapper.find('a[children="Duplicate"]');
    WidgetActions.duplicate = mockAction(jest.fn(() => Promise.resolve(WidgetModel.builder().id('duplicatedWidgetId').build())));
    TitlesActions.set = mockAction(jest.fn((type, id, title) => {
      expect(type).toEqual(TitleTypes.Widget);
      expect(id).toEqual('duplicatedWidgetId');
      expect(title).toEqual('Dummy Widget (copy)');
      done();
      return Promise.resolve();
    }));

    duplicate.simulate('click');

    expect(WidgetActions.duplicate).toHaveBeenCalled();
  });

  describe('copy widget to dashboard', () => {
    beforeEach(() => {
      DashboardsStore.getInitialState = jest.fn(() => dashboardState);
      ViewStore.getInitialState = jest.fn(() => viewStoreState);
      ViewManagementActions.get = mockAction(jest.fn((() => Promise.resolve(dashboard1.toJSON()))));
      SearchActions.get = mockAction(jest.fn(() => Promise.resolve(searchDB1.toJSON())));
      ViewManagementActions.update = mockAction(jest.fn(() => Promise.resolve()));
      SearchActions.create = mockAction(jest.fn(() => Promise.resolve({ search: searchDB1.toJSON() })));
      Routes.pluginRoute = jest.fn(route => id => `${route}-${id}`);
      browserHistory.push = jest.fn();
      asMock(CopyWidgetToDashboard).mockImplementation(() => View.builder()
        .search(Search.builder().id('search-id').build())
        .id('new-id').type(View.Type.Dashboard)
        .build());
    });
    afterEach(() => {
      cleanup();
    });
    const renderAndClick = () => {
      const { getByText, getByTestId } = render(<DummyWidget />);
      const actionToggle = getByTestId('widgetActionDropDown');
      fireEvent.click(actionToggle);
      const copyToDashboard = getByText('Copy to Dashboard');
      fireEvent.click(copyToDashboard);
      const view1ListItem = getByText('view 1');
      fireEvent.click(view1ListItem);
      const selectBtn = getByText('Select');
      fireEvent.click(selectBtn);
    };

    it('should get dashboard from backend', async () => {
      renderAndClick();
      await wait(() => expect(ViewManagementActions.get).toHaveBeenCalledTimes(1));
    });
    it('should get corresponding search to dashboard', async () => {
      renderAndClick();
      await wait(() => expect(SearchActions.get).toHaveBeenCalledTimes(1));
    });
    it('should create new search for dashboard', async () => {
      renderAndClick();
      await wait(() => expect(SearchActions.create).toHaveBeenCalledTimes(1));
    });
    it('should update dashboard with new search and widget', async () => {
      renderAndClick();
      await wait(() => expect(ViewManagementActions.update).toHaveBeenCalledTimes(1));
    });
    it('should redirect to updated dashboard', async () => {
      renderAndClick();
      await wait(() => expect(browserHistory.push).toHaveBeenCalledTimes(1));
    });
  });
  it('adds cancel action to widget in edit mode', () => {
    const wrapper = mount(<DummyWidget editing />);
    const cancel = wrapper.find('Button[children="Cancel"]');
    expect(cancel).toExist();
  });
  it('does not trigger action when clicking cancel after no changes were made', () => {
    const wrapper = mount(<DummyWidget editing />);

    WidgetActions.updateConfig = mockAction(jest.fn());

    const cancelButton = wrapper.find('Button[children="Cancel"]').at(0);
    cancelButton.simulate('click');

    expect(WidgetActions.updateConfig).not.toHaveBeenCalled();
  });
  it('restores original state of widget config when clicking cancel after changes were made', () => {
    const widgetWithConfig = { config: { foo: 42 }, id: 'widgetId', type: 'dummy' };
    const wrapper = mount(<DummyWidget editing widget={widgetWithConfig} />);

    const editComponent = wrapper.find('edit-dummy-visualization').at(0);
    const widgetConfigChange = editComponent.props().onChange;

    WidgetActions.updateConfig = mockAction(jest.fn());
    widgetConfigChange({ foo: 23 });
    expect(WidgetActions.updateConfig).toHaveBeenCalledWith('widgetId', { foo: 23 });

    const cancelButton = wrapper.find('button[children="Cancel"]');
    cancelButton.simulate('click');

    expect(WidgetActions.updateConfig).toHaveBeenCalledWith('widgetId', { foo: 42 });
  });
  it('does not restores original state of widget config when clicking "Finish Editing"', () => {
    const widgetWithConfig = { config: { foo: 42 }, id: 'widgetId', type: 'dummy' };
    const wrapper = mount(<DummyWidget editing widget={widgetWithConfig} />);

    const editComponent = wrapper.find('edit-dummy-visualization').at(0);
    const widgetConfigChange = editComponent.props().onChange;

    WidgetActions.updateConfig = mockAction(jest.fn());
    widgetConfigChange({ foo: 23 });
    expect(WidgetActions.updateConfig).toHaveBeenCalledWith('widgetId', { foo: 23 });

    const cancelButton = wrapper.find('button[children="Save"]');
    cancelButton.simulate('click');

    expect(WidgetActions.updateConfig).not.toHaveBeenCalledWith('widgetId', { foo: 42 });
  });
});
