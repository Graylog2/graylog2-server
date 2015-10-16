import React from 'react';

import { Row, Col } from 'react-bootstrap';

import PageHeader from 'components/common/PageHeader';
import CollectorList from 'components/collectors/CollectorList';

const CollectorsPage = React.createClass({
  render() {
    return (
      <span>
        <PageHeader title="Collectors in Cluster">
          <span>
            The Graylog collectors can reliably forward contents of log files or Windows EventLog from your servers.
          </span>

          <span>
            Read more about collectors and how to set them up in the
            @views.html.partials.links.docs(views.helpers.DocsHelper.PAGE_COLLECTOR, "Graylog documentation").
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <CollectorList />
          </Col>
        </Row>
      </span>
    );
  }
});

export default CollectorsPage;
