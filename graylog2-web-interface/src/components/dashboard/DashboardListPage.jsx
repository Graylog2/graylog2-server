import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import Immutable from 'immutable';
import { Row, Col } from 'react-bootstrap';

import CombinedProvider from 'injection/CombinedProvider';

const { DashboardsActions, DashboardsStore } = CombinedProvider.get('Dashboards');

import DocsHelper from 'util/DocsHelper';
import PermissionsMixin from 'util/PermissionsMixin';

import DocumentationLink from 'components/support/DocumentationLink';
import Spinner from 'components/common/Spinner';
import PageHeader from 'components/common/PageHeader';
import DashboardList from './DashboardList';
import EditDashboardModalTrigger from './EditDashboardModalTrigger';

const DashboardListPage = createReactClass({
  displayName: 'DashboardListPage',

  propTypes: {
    permissions: PropTypes.arrayOf(PropTypes.string),
  },

  mixins: [Reflux.connect(DashboardsStore, 'dashboards'), PermissionsMixin],

  getInitialState() {
    return {
      dashboardsLoaded: false,
    };
  },

  componentDidMount() {
    DashboardsActions.list();
  },

  render() {
    const { dashboards } = this.state.dashboards;
    const filteredDashboards = dashboards;
    const createDashboardButton = this.isPermitted(this.props.permissions, ['dashboards:create']) ?
      <EditDashboardModalTrigger action="create" buttonClass="btn-success btn-lg" /> : null;

    const pageHeader = (
      <PageHeader title="Dashboards">
        <span>
          Use dashboards to create specific views on your messages. Create a new dashboard here and add
          any graph or chart you create in other parts of Graylog with one click.
        </span>

        <span>
          Take a look at the
          {' '}<DocumentationLink page={DocsHelper.PAGES.DASHBOARDS} text="dashboard tutorial" />{' '}
          for lots of other useful tips.
        </span>

        {createDashboardButton}
      </PageHeader>
    );

    let dashboardList;

    if (!dashboards) {
      dashboardList = <Spinner />;
    } else {
      if (dashboards && dashboards.count() > 0 && filteredDashboards.isEmpty()) {
        dashboardList = <div>No dashboards matched your filter criteria.</div>;
      } else {
        dashboardList = (
          <DashboardList dashboards={filteredDashboards}
                         onDashboardAdd={this._onDashboardAdd}
                         permissions={this.props.permissions}/>
        );
      }
    }

    return (
      <div>
        {pageHeader}

        <Row className="content">
          <Col md={12}>
            {dashboardList}
          </Col>
        </Row>
      </div>
    );
  },
});

export default DashboardListPage;
