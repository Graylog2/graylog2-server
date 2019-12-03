import 'jquery-ui/ui/version';
import 'jquery-ui/ui/effect';
import 'jquery-ui/ui/plugin';
import 'jquery-ui/ui/widget';
import 'jquery-ui/ui/widgets/mouse';
import React from 'react';
import Immutable from 'immutable';
import { mount } from 'theme/enzymeWithTheme';

import { StoreMock as MockStore } from 'helpers/mocking';
import PermissionsMixin from 'util/PermissionsMixin';
import AddToDashboardMenu from './AddToDashboardMenu';

jest.mock('stores/dashboards/DashboardsStore', () => MockStore('listen'));
jest.mock('stores/widgets/WidgetsStore', () => ({
  addWidget: jest.fn(),
}));
const mockUser = { timezone: 'UTC' };
jest.mock('stores/users/CurrentUserStore', () => MockStore('listen', ['get', () => mockUser], ['getInitialState', () => ({ mockUser })]));

describe('<AddToDashboardMenu />', () => {
  const exampleProps = {
    widgetType: 'SEARCH_RESULT_CHART',
    title: 'Add to dashboard',
    permissions: [],
  };

  const allDashboards = Immutable.List([
    { id: '1', title: 'One' },
    { id: '2', title: 'Two' },
    { id: '3', title: 'Three' },
  ]).sortBy(dashboard => dashboard.title);

  const filterDashboards = (dashboards, permissions) => ({
    // Emulate API behaviour by filtering out dashboards the user cannot read
    dashboards: dashboards.filter(dashboard => PermissionsMixin.isPermitted(permissions, `dashboards:read:${dashboard.id}`)),
    writableDashboards: dashboards.filter(dashboard => PermissionsMixin.isPermitted(permissions, `dashboards:edit:${dashboard.id}`)),
  });

  describe('admin users', () => {
    let wrapper;
    const permissions = ['*'];
    beforeEach(() => {
      wrapper = mount(<AddToDashboardMenu widgetType={exampleProps.widgetType}
                                          title={exampleProps.title}
                                          permissions={permissions} />);
    });

    it('should see all dashboards', () => {
      const { dashboards, writableDashboards } = filterDashboards(allDashboards, permissions);
      wrapper.setState({ dashboards: dashboards, writableDashboards: writableDashboards });
      wrapper.find('.dropdown-menu').find('MenuItem').forEach((node, idx) => {
        expect(node.prop('children')).toEqual(writableDashboards.get(idx).title);
      });
    });

    it('should see an option to create a new dashboard when there are none', () => {
      const { dashboards, writableDashboards } = filterDashboards(Immutable.List(), permissions);
      wrapper.setState({ dashboards: dashboards, writableDashboards: writableDashboards });
      expect(wrapper.find('.dropdown-menu').find('MenuItem').prop('children')).toEqual('No dashboards, create one?');
    });
  });

  describe('reader users', () => {
    describe('without dashboards:create permissions', () => {
      let wrapper;
      const permissions = ['dashboards:read:1', 'dashboards:read:2', 'dashboards:edit:2', 'dashboards:read:3'];
      beforeEach(() => {
        wrapper = mount(<AddToDashboardMenu widgetType={exampleProps.widgetType}
                                            title={exampleProps.title}
                                            permissions={permissions} />);
      });

      it('should only see the dashboards that can edit', () => {
        const { dashboards, writableDashboards } = filterDashboards(allDashboards, permissions);
        wrapper.setState({ dashboards: dashboards, writableDashboards: writableDashboards });
        expect(wrapper.find('.dropdown-menu').find('MenuItem').length).toEqual(1);
        expect(wrapper.find('.dropdown-menu').find('MenuItem').prop('children')).toEqual('Two');
      });

      it('should NOT see an option to create a new dashboard when there are none', () => {
        const { dashboards, writableDashboards } = filterDashboards(Immutable.List(), permissions);
        wrapper.setState({ dashboards: dashboards, writableDashboards: writableDashboards });
        expect(wrapper.find('.dropdown-menu').find('MenuItem').prop('children')).not.toEqual('No dashboards, create one?');
        expect(wrapper.find('.dropdown-menu').find('MenuItem').prop('children')).toEqual('No dashboards available');
      });
    });

    describe('with dashboards:create permissions', () => {
      let wrapper;
      const permissions = ['dashboards:create', 'dashboards:read:1', 'dashboards:read:2', 'dashboards:edit:2', 'dashboards:read:3'];
      beforeEach(() => {
        wrapper = mount(<AddToDashboardMenu widgetType={exampleProps.widgetType}
                                            title={exampleProps.title}
                                            permissions={permissions} />);
      });

      it('should only see the dashboards that can edit', () => {
        const { dashboards, writableDashboards } = filterDashboards(allDashboards, permissions);
        wrapper.setState({ dashboards: dashboards, writableDashboards: writableDashboards });
        expect(wrapper.find('.dropdown-menu').find('MenuItem').length).toEqual(1);
        expect(wrapper.find('.dropdown-menu').find('MenuItem').prop('children')).toEqual('Two');
      });

      it('should see an option to create a new dashboard when there are none', () => {
        const { dashboards, writableDashboards } = filterDashboards(Immutable.List(), permissions);
        wrapper.setState({ dashboards: dashboards, writableDashboards: writableDashboards });
        expect(wrapper.find('.dropdown-menu').find('MenuItem').prop('children')).toEqual('No dashboards, create one?');
      });
    });
  });
});
