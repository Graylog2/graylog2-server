import React from 'react';
import { Button, Col, Row } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';

import DocsHelper from 'util/DocsHelper';
import { DocumentTitle, PageHeader } from 'components/common';
import { DocumentationLink } from 'components/support';
import { IndexSetsComponent } from 'components/indices';

const IndicesPage = React.createClass({
  render() {
    const pageHeader = (
      <PageHeader title="Indices & Index Sets">
        <span>
          A Graylog stream write messages to an index set, which is a configuration for retention, sharding, and
          replication of the stored data.
          By configuring index sets, you could, for example, have different retention times for certain streams.
        </span>

        <span>
          You can learn more about the index model in the{' '}
          <DocumentationLink page={DocsHelper.PAGES.INDEX_MODEL} text="documentation" />
        </span>

        <span>
          <LinkContainer to={Routes.SYSTEM.INDEX_SETS.CREATE}>
            <Button bsStyle="success" bsSize="lg">Create index set</Button>
          </LinkContainer>
        </span>
      </PageHeader>
    );

    return (
      <DocumentTitle title="Indices and Index Sets">
        <span>
          {pageHeader}

          <Row className="content">
            <Col md={12}>
              <IndexSetsComponent />
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  },
});

export default IndicesPage;
