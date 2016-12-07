import React from 'react';
import Reflux from 'reflux';
import { Button, Col, Row } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import { AlertsComponent } from 'components/alerts';

import DocumentationLink from 'components/support/DocumentationLink';
import { DocumentTitle, PageHeader } from 'components/common';

import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';

import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const AlertsPage = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore)],
  render() {
    return (
      <DocumentTitle title="Alerts">
        <div>
          <PageHeader title="Alerts overview">
            <span>
              Alerts are triggered when conditions you define are satisfied. Graylog will automatically mark alerts as
              resolved once the status of your conditions change.
            </span>

            <span>
              Read more about alerting in the <DocumentationLink page={DocsHelper.PAGES.ALERTS} text="documentation" />.
            </span>

            <span>
              <LinkContainer to={Routes.ALERTS.CONDITIONS}>
                <Button bsStyle="info">Manage conditions</Button>
              </LinkContainer>
              &nbsp;
              <LinkContainer to={Routes.ALERTS.NOTIFICATIONS}>
                <Button bsStyle="info">Manage notifications</Button>
              </LinkContainer>
            </span>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <AlertsComponent />
            </Col>
          </Row>
        </div>
      </DocumentTitle>
    );
  },
});

export default AlertsPage;
