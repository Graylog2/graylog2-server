import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Button, Col, Row } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';
import { DocumentTitle, PageHeader } from 'components/common/index';

import { ViewManagementStore, ViewManagementActions } from 'enterprise/stores/ViewManagementStore';
import ViewList from 'enterprise/components/views/ViewList';

const ViewManagementPage = createReactClass({
  mixins: [
    Reflux.connect(ViewManagementStore, 'views'),
  ],

  handleSearch(query, page, perPage) {
    return ViewManagementActions.search(query, page, perPage);
  },

  handleViewDelete(view) {
    return ViewManagementActions.delete(view);
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
              <LinkContainer to={Routes.pluginRoute('EXTENDEDSEARCH')}>
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
