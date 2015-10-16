import React from 'react';

import { Row, Col } from 'react-bootstrap';

import DocsHelper from 'util/DocsHelper';

import PageHeader from 'components/common/PageHeader';
import CollectorList from 'components/collectors/CollectorList';
import DocumentationLink from 'components/support/DocumentationLink';

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
            {' '}<DocumentationLink page={DocsHelper.PAGES.COLLECTOR} text="Graylog documentation"/>.
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
