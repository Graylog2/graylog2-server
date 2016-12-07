import React from 'react';
import Reflux from 'reflux';
import { Button, Col, Row } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import { DocumentTitle, PageHeader } from 'components/common';
import { AlertNotificationsComponent } from 'components/alertnotifications';
import Routes from 'routing/Routes';

import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const AlertNotificationsPage = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore)],
  render() {
    return (
      <DocumentTitle title="Alert notifications">
        <div>
          <PageHeader title="Manage alert notifications">
            <span>
              Notifications let you be aware of changes in your alert conditions status any time. Graylog can send
              notifications directly to you or to other systems you use for that purpose.
            </span>

            <span>
              Remember to assign the notifications to use in the alert conditions page.
            </span>

            <span>
              <LinkContainer to={Routes.ALERTS.CONDITIONS}>
                <Button bsStyle="info">Manage conditions</Button>
              </LinkContainer>
              &nbsp;
              <Button bsStyle="info" href="https://marketplace.graylog.org/" target="_blank">
                <i className="fa fa-external-link" />&nbsp; Find more notifications
              </Button>
            </span>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <AlertNotificationsComponent />
            </Col>
          </Row>
        </div>
      </DocumentTitle>
    );
  },
});

export default AlertNotificationsPage;
