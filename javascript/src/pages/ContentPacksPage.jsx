import React from 'react';
import { Row, Col } from 'react-bootstrap';

import PageHeader from 'components/common/PageHeader';
import ConfigurationBundles from 'components/source-tagging/ConfigurationBundles';

const ContentPacksPage = React.createClass({
  render() {

    return (
      <span>
        <PageHeader title="Content packs">
          <span>
            Content packs accelerate the set up process for a specific data source. A content pack can include inputs/extractors, streams, and dashboards.
          </span>

          <span>
            Find more content packs in {' '}
            <a href="https://marketplace.graylog.org/" target="_blank">the Graylog Marketplace</a>.
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>

            <h2>Select content packs</h2>
            <div id="react-configuration-bundles">
              <ConfigurationBundles/>
            </div>
          </Col>
        </Row>
      </span>
    );
  }
});

export default ContentPacksPage;
