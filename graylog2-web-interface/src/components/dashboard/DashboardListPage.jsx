import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';

import CombinedProvider from 'injection/CombinedProvider';
import DocsHelper from 'util/DocsHelper';
import PermissionsMixin from 'util/PermissionsMixin';

import { PaginatedList, SearchForm } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import Spinner from 'components/common/Spinner';
import PageHeader from 'components/common/PageHeader';
import DashboardList from './DashboardList';
import EditDashboardModalTrigger from './EditDashboardModalTrigger';

const { DashboardsActions, DashboardsStore } = CombinedProvider.get('Dashboards');

const DashboardListPage = createReactClass({
  displayName: 'DashboardListPage',

  propTypes: {
    permissions: PropTypes.arrayOf(PropTypes.string).isRequired,
  },

  mixins: [Reflux.connect(DashboardsStore), PermissionsMixin],

  getInitialState() {
    return {
      dashboardsLoaded: false,
    };
  },

  componentDidMount() {
    this.loadData();
  },

  loadData(callback) {
    const { page, perPage, query } = this.state.pagination;
    DashboardsActions.listPage(page, perPage, query).then(() => {
      if (callback) {
        callback();
      }
    });
  },

  _onPageChange(newPage, newPerPage) {
    const pagination = this.state.pagination;
    const newPagination = Object.assign(pagination, {
      page: newPage,
      perPage: newPerPage,
    });
    this.setState({ pagination, newPagination }, this.loadData);
  },

  _onSearch(query, resetLoadingCallback) {
    const pagination = this.state.pagination;
    const newPagination = Object.assign(pagination, { query: query });
    this.setState({ pagination, newPagination }, () => this.loadData(resetLoadingCallback));
  },

  _onReset() {
    const pagination = this.state.pagination;
    const newPagination = Object.assign(pagination, { query: '' });
    this.setState({ pagination, newPagination }, this.loadData);
  },

  render() {
    const { dashboards } = this.state;
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
      dashboardList = (
        <DashboardList dashboards={this.state.dashboards}
                       permissions={this.props.permissions} />
      );
    }

    return (
      <div>
        {pageHeader}

        <Row className="content">
          <Col md={12}>
            <Row className="row-sm">
              <Col md={8}>
                <SearchForm onSearch={this._onSearch} onReset={this._onReset} useLoadingState />
              </Col>
            </Row>
            <Row>
              <Col md={12}>
                <PaginatedList onChange={this._onPageChange} totalItems={this.state.pagination.total}>
                  <br />
                  <br />
                  {dashboardList}
                </PaginatedList>
              </Col>
            </Row>
          </Col>
        </Row>
      </div>
    );
  },
});

export default DashboardListPage;
