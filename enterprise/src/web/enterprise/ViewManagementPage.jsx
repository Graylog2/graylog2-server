import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Button, Col, Row } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';
import { DocumentTitle, PageHeader } from 'components/common';

import { ViewStore, ViewActions } from 'enterprise/stores/ViewManagementStore';
import ViewList from 'enterprise/components/views/ViewList';

const ViewManagementPage = createReactClass({
  mixins: [Reflux.connect(ViewStore, 'views')],

  handleSearch(query, page, perPage) {
    return ViewActions.search(query, page, perPage);
  },

  handleViewExecution(view) {
    return ViewActions.execute(view);
  },

  handleViewDelete(view) {
    return ViewActions.delete(view);
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
              <LinkContainer to={Routes.pluginRoute('VIEWS')}>
                <Button bsStyle="success" bsSize="lg">Create new view</Button>
              </LinkContainer>
            </span>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <ViewList views={this.state.views.list}
                        pagination={this.state.views.pagination}
                        handleSearch={this.handleSearch}
                        handleViewExecution={this.handleViewExecution}
                        handleViewDelete={this.handleViewDelete} />
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  },
});

export default ViewManagementPage;
