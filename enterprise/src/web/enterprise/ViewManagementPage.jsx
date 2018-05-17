import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Button, Col, Row } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';
import { DocumentTitle, PageHeader } from 'components/common';

import { ViewManagementStore, ViewManagementActions } from 'enterprise/stores/ViewManagementStore';
import ViewList from 'enterprise/components/views/ViewList';

const ViewManagementPage = createReactClass({
  mixins: [Reflux.connect(ViewManagementStore, 'views')],

  handleSearch(query, page, perPage) {
    return ViewManagementActions.search(query, page, perPage);
  },

  handleViewDelete(view) {
    return ViewManagementActions.delete(view);
  },

  render() {
    return (
      <DocumentTitle title="Views">
        <span>
          <PageHeader title="Views">
            <span>
               Graylog view management.
            </span>

            {null}

            <span>
              <LinkContainer to={Routes.pluginRoute('EXTENDEDSEARCH')}>
                <Button bsStyle="success" bsSize="lg">Create new view</Button>
              </LinkContainer>
            </span>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <ViewList views={this.state.views.list}
                        pagination={this.state.views.pagination}
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
