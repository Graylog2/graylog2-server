import React from 'react';

import { Button, ButtonToolbar, Col, Row } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import DocsHelper from 'util/DocsHelper';

import { DocumentTitle, PageHeader } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import SidecarListContainer from 'components/sidecars/sidecars/SidecarListContainer';

import Routes from 'routing/Routes';

const SidecarsPage = React.createClass({
  render() {
    return (
      <DocumentTitle title="Sidecars">
        <span>
          <PageHeader title="Sidecars Overview">
            <span>
              The Graylog sidecars can reliably forward contents of log files or Windows EventLog from your servers.
            </span>

            <span>
              Read more about sidecars and how to set them up in the
              {' '}<DocumentationLink page={DocsHelper.PAGES.COLLECTOR_SIDECAR} text="Graylog documentation" />.
            </span>

            <ButtonToolbar>
              <LinkContainer to={Routes.SYSTEM.SIDECARS.OVERVIEW}>
                <Button bsStyle="info" className="active">Overview</Button>
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
              <SidecarListContainer />
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  },
});

export default SidecarsPage;
