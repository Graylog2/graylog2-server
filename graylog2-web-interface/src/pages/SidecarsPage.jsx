import React from 'react';
import { Button, ButtonToolbar, Col, Row } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import { Link } from 'react-router';

import { DocumentTitle, PageHeader } from 'components/common';
import SidecarListContainer from 'components/sidecars/sidecars/SidecarListContainer';

import Routes from 'routing/Routes';

class SidecarsPage extends React.Component {
  render() {
    return (
      <DocumentTitle title="Sidecars">
        <span>
          <PageHeader title="Sidecars Overview">
            <span>
              The Graylog sidecars can reliably forward contents of log files or Windows EventLog from your servers.
            </span>

            <span>
              Do you need an API token for a sidecar?&ensp;
              <Link to={Routes.SYSTEM.AUTHENTICATION.USERS.TOKENS.edit('graylog-sidecar')}>
                Create or reuse a token for the <em>graylog-sidecar</em> user
              </Link>.
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
  }
}

export default SidecarsPage;
