// @flow strict
import * as React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
// $FlowFixMe: imports from core need to be fixed in flow
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';
import { Col, Row, Button } from 'components/graylog';
import { DocumentTitle, PageHeader } from 'components/common';

import { ViewManagementStore, ViewManagementActions } from 'views/stores/ViewManagementStore';
import ViewList from 'views/components/views/ViewList';

const ViewManagementPage = createReactClass({
  mixins: [
    Reflux.connect(ViewManagementStore, 'views'),
  ],

  handleSearch(query, page, perPage) {
    return ViewManagementActions.search(query, page, perPage);
  },


  handleViewDelete(view) {
    // eslint-disable-next-line no-alert
    if (window.confirm(`Are you sure you want to delete "${view.title}"?`)) {
      return ViewManagementActions.delete(view);
    }
    return null;
  },

  render() {
    const { views } = this.state;

    return (
      <DocumentTitle title="Views">
        <span>
          <PageHeader title="Views">
            <span>
              Graylog view management.
            </span>

            {null}

            <span>
              <LinkContainer to={Routes.EXTENDEDSEARCH}>
                <Button bsStyle="success" bsSize="lg">Create new view</Button>
              </LinkContainer>
            </span>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <ViewList views={views.list}
                        pagination={views.pagination}
                        handleSearch={this.handleSearch}
                        handleViewDelete={this.handleViewDelete} />
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  },
});

export default ViewManagementPage;
