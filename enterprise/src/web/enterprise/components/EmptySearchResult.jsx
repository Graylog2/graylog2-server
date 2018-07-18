import React from 'react';
import { Col, Row } from 'react-bootstrap';

import { ContactUs, DocumentationLink } from 'components/support';
import DocsHelper from 'util/DocsHelper';

const EmptySearchResult = () => (
  <div style={{ marginLeft: '-20px' }}>
    <Row className="content content-head">
      <Col md={12}>
        <h1>Nothing found.</h1>

        <p className="description">
          Your search returned no results, try changing the used time range or the search query.{' '}
          <br />
          <strong>
            Take a look at the{' '}
            <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE} text="documentation" />{' '}
            if you need help with the search syntax or the time range selector.
          </strong>
        </p>
      </Col>
    </Row>
    <ContactUs />
  </div>
);

EmptySearchResult.propTypes = {};

export default EmptySearchResult;
