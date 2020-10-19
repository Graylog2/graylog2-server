// @flow strict
import * as React from 'react';
import { useState, useEffect, useContext } from 'react';

import withParams from 'routing/withParams';
import { LinkContainer } from 'components/graylog/router';
import { ButtonToolbar, Col, Row, Button } from 'components/graylog';
import CombinedProvider from 'injection/CombinedProvider';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import CurrentUserContext from 'contexts/CurrentUserContext';
import DocumentationLink from 'components/support/DocumentationLink';
import { isPermitted } from 'util/PermissionsMixin';
import history from 'util/History';

const { EventDefinitionsActions } = CombinedProvider.get('EventDefinitions');

type Props = {
  params: {
    definitionId: string,
  },
};

const ViewEventDefinitionPage = ({ params }: Props) => {
  const currentUser = useContext(CurrentUserContext);
  const [eventDefinition, setEventDefinition] = useState();

  useEffect(() => {
    if (currentUser && isPermitted(currentUser.permissions, `eventdefinitions:read:${params.definitionId}`)) {
      EventDefinitionsActions.get(params.definitionId)
        .then(
          (response) => {
            const eventDefinitionResp = response.event_definition;

            // Inject an internal "_is_scheduled" field to indicate if the event definition should be scheduled in the
            // backend. This field will be removed in the event definitions store before sending an event definition
            // back to the server.
            eventDefinitionResp.config._is_scheduled = response.context.scheduler.is_scheduled;
            setEventDefinition(eventDefinitionResp);
          },
          (error) => {
            if (error.status === 404) {
              history.push(Routes.ALERTS.DEFINITIONS.LIST);
            }
          },
        );
    }
  }, [currentUser, params]);

  if (!eventDefinition) {
    return (
      <DocumentTitle title="Edit Event Definition">
        <span>
          <PageHeader title="Edit Event Definition">
            <Spinner text="Loading Event Definition..." />
          </PageHeader>
        </span>
      </DocumentTitle>
    );
  }

  return (
    <DocumentTitle title={`Edit "${eventDefinition.title}" Event Definition`}>
      <span>
        <PageHeader title={`Edit "${eventDefinition.title}" Event Definition`}>
          <span>
            Event Definitions allow you to create Events from different Conditions and alert on them.
          </span>

          <span>
            Graylog&apos;s new Alerting system let you define more flexible and powerful rules. Learn more in the{' '}
            <DocumentationLink page={DocsHelper.PAGES.ALERTS}
                               text="documentation" />
          </span>

          <ButtonToolbar>
            <LinkContainer to={Routes.ALERTS.LIST}>
              <Button bsStyle="info">Alerts & Events</Button>
            </LinkContainer>
            <LinkContainer to={Routes.ALERTS.DEFINITIONS.LIST}>
              <Button bsStyle="info">Event Definitions</Button>
            </LinkContainer>
            <LinkContainer to={Routes.ALERTS.NOTIFICATIONS.LIST}>
              <Button bsStyle="info">Notifications</Button>
            </LinkContainer>
          </ButtonToolbar>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
          </Col>
        </Row>
      </span>
    </DocumentTitle>
  );
};

export default withParams(ViewEventDefinitionPage);
