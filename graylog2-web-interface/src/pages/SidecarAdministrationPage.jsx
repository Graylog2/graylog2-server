import React from 'react';
import PropTypes from 'prop-types';

import { LinkContainer } from 'components/graylog/router';
import { ButtonToolbar, Col, Row, Button } from 'components/graylog';
import DocsHelper from 'util/DocsHelper';
import { DocumentTitle, PageHeader } from 'components/common';
import Routes from 'routing/Routes';
import DocumentationLink from 'components/support/DocumentationLink';
import CollectorsAdministrationContainer from 'components/sidecars/administration/CollectorsAdministrationContainer';
import withLocation from 'routing/withLocation';

const SidecarAdministrationPage = ({ location: { query: { node_id: nodeId } } }) => (
  <DocumentTitle title="Collectors Administration">
    <span>
      <PageHeader title="Collectors Administration">
        <span>
          The Graylog collectors can reliably forward contents of log files or Windows EventLog from your servers.
        </span>

        <span>
          Read more about collectors and how to set them up in the
          {' '}<DocumentationLink page={DocsHelper.PAGES.COLLECTOR} text="Graylog documentation" />.
        </span>

        <ButtonToolbar>
          <LinkContainer to={Routes.SYSTEM.SIDECARS.OVERVIEW}>
            <Button bsStyle="info">Overview</Button>
          </LinkContainer>
          <LinkContainer to={Routes.SYSTEM.SIDECARS.ADMINISTRATION}>
            <Button bsStyle="info">Administration</Button>
          </LinkContainer>
          <LinkContainer to={Routes.SYSTEM.SIDECARS.CONFIGURATION}>
            <Button bsStyle="info">Configuration</Button>
          </LinkContainer>
        </ButtonToolbar>
      </PageHeader>

      <Row className="content">
        <Col md={12}>
          <CollectorsAdministrationContainer nodeId={nodeId} />
        </Col>
      </Row>
    </span>
  </DocumentTitle>
);

SidecarAdministrationPage.propTypes = {
  location: PropTypes.object.isRequired,
};

export default withLocation(SidecarAdministrationPage);
