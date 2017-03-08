import React from 'react';
import { Row, Col, Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';

import { DocumentTitle, PageHeader } from 'components/common';
import ConfigurationBundles from 'components/source-tagging/ConfigurationBundles';

const ContentPacksPage = React.createClass({
  render() {
    return (
      <DocumentTitle title="Content packs">
        <span>
          <PageHeader title="Content packs">
            <span>
              Content packs accelerate the set up process for a specific data source. A content pack can include inputs/extractors, streams, and dashboards.
            </span>

            <span>
              Find more content packs in {' '}
              <a href="https://marketplace.graylog.org/" target="_blank">the Graylog Marketplace</a>.
            </span>

            <LinkContainer to={Routes.SYSTEM.CONTENTPACKS.EXPORT}>
              <Button bsStyle="success" bsSize="large">Create a content pack</Button>
            </LinkContainer>
          </PageHeader>

          <Row className="content">
            <Col md={12}>

              <h2>Select content packs</h2>
              <div id="react-configuration-bundles">
                <ConfigurationBundles />
              </div>
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  },
});

export default ContentPacksPage;
