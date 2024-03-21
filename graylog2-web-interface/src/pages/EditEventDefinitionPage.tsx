/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React, { useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import URI from 'urijs';

import EventsPageNavigation from 'components/events/EventsPageNavigation';
import { Col, Row } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import EventDefinitionFormContainer from 'components/event-definitions/event-definition-form/EventDefinitionFormContainer';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import { isPermitted } from 'util/PermissionsMixin';
import useCurrentUser from 'hooks/useCurrentUser';
import { EventDefinitionsActions } from 'stores/event-definitions/EventDefinitionsStore';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';
import useHistory from 'routing/useHistory';
import useQuery from 'routing/useQuery';

import StreamPermissionErrorPage from './StreamPermissionErrorPage';

const EditEventDefinitionPage = () => {
  const params = useParams<{definitionId?: string}>();
  const { step } = useQuery();
  const currentUser = useCurrentUser();
  const [eventDefinition, setEventDefinition] = React.useState<EventDefinition>(undefined);
  const history = useHistory();
  const navigate = useNavigate();

  const goToOverview = useCallback(() => {
    navigate(Routes.ALERTS.DEFINITIONS.LIST);
  }, [navigate]);

  React.useEffect(() => {
    if (isPermitted(currentUser.permissions, `eventdefinitions:edit:${params.definitionId}`)) {
      EventDefinitionsActions.get(params.definitionId)
        .then(
          (response) => {
            const eventDefinitionResponse = response.event_definition;

            // Inject an internal "_is_scheduled" field to indicate if the event definition should be scheduled in the
            // backend. This field will be removed in the event definitions store before sending an event definition
            // back to the server.
            eventDefinitionResponse.config._is_scheduled = response.context.scheduler.is_scheduled;
            setEventDefinition(eventDefinitionResponse);
          },
          (error) => {
            if (error.status === 404) {
              history.push(Routes.ALERTS.DEFINITIONS.LIST);
            }
          },
        );
    }
  }, [params, currentUser, history]);

  const streamsWithMissingPermissions = () => {
    const streams = eventDefinition?.config?.streams || [];

    return streams.filter((streamId) => !isPermitted(currentUser.permissions, `streams:read:${streamId}`));
  };

  if (!isPermitted(currentUser.permissions, `eventdefinitions:edit:${params.definitionId}`)) {
    history.push(Routes.NOTFOUND);
  }

  const missingStreams = streamsWithMissingPermissions();

  const updateURLStepQueryParam = (newStep: string) => {
    const newUrl = new URI(window.location.href).removeSearch('step').addQuery('step', newStep);
    history.replace(newUrl.resource());
  };

  if (missingStreams.length > 0) {
    return <StreamPermissionErrorPage error={null} missingStreamIds={missingStreams} />;
  }

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
      <EventsPageNavigation />
      <PageHeader title={`Edit "${eventDefinition.title}" Event Definition`}
                  documentationLink={{
                    title: 'Alerts documentation',
                    path: DocsHelper.PAGES.ALERTS,
                  }}>
        <span>
          Event Definitions allow you to create Events from different Conditions and alert on them.
        </span>
      </PageHeader>
      <Row className="content">
        <Col md={12}>
          <EventDefinitionFormContainer action="edit"
                                        initialStep={step as string}
                                        onChangeStep={updateURLStepQueryParam}
                                        eventDefinition={eventDefinition}
                                        onSubmit={goToOverview}
                                        onCancel={goToOverview} />
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default EditEventDefinitionPage;
