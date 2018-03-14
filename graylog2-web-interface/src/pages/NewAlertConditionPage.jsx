import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Col, Row } from 'react-bootstrap';

import DocumentationLink from 'components/support/DocumentationLink';
import { DocumentTitle, PageHeader } from 'components/common';
import { AlertsHeaderToolbar } from 'components/alerts';
import { CreateAlertConditionInput } from 'components/alertconditions';

import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';

import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const NewAlertConditionPage = createReactClass({
  displayName: 'NewAlertConditionPage',
  mixins: [Reflux.connect(CurrentUserStore)],

  render() {
    return (
      <DocumentTitle title="New alert condition">
        <div>
          <PageHeader title="New alert condition">
            <span>
              Define an alert condition and configure the way Graylog will notify you when that condition is satisfied.
            </span>

            <span>
              Are the default conditions not flexible enough? You can write your own! Read more about alerting in
              the{' '}
              <DocumentationLink page={DocsHelper.PAGES.ALERTS} text="documentation" />.
            </span>

            <span>
              <AlertsHeaderToolbar active={Routes.ALERTS.CONDITIONS} />
            </span>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <CreateAlertConditionInput />
            </Col>
          </Row>
        </div>
      </DocumentTitle>
    );
  },
});

export default NewAlertConditionPage;
