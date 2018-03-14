import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Col, Row } from 'react-bootstrap';

import { DocumentTitle, PageHeader } from 'components/common';
import { AlertsHeaderToolbar } from 'components/alerts';
import { CreateAlertNotificationInput } from 'components/alertnotifications';

import Routes from 'routing/Routes';

import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const NewAlertNotificationPage = createReactClass({
  displayName: 'NewAlertNotificationPage',
  mixins: [Reflux.connect(CurrentUserStore)],

  render() {
    return (
      <DocumentTitle title="New alert notification">
        <div>
          <PageHeader title="New alert notification">
            <span>
              Create a new notification that you can use to not miss any of your alerts.
            </span>

            <span>
              Remember to assign the notifications to use in the alert conditions page.
            </span>

            <span>
              <AlertsHeaderToolbar active={Routes.ALERTS.NOTIFICATIONS} />
            </span>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <CreateAlertNotificationInput />
            </Col>
          </Row>
        </div>
      </DocumentTitle>
    );
  },
});

export default NewAlertNotificationPage;
