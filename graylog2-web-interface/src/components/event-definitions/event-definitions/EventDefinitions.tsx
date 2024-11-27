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
import React from 'react';

import { LinkContainer } from 'components/common/router';
import {
  EmptyEntity,
  EntityList,
  IfPermitted,
  PaginatedList,
  SearchForm,
} from 'components/common';
import { Button, Col, Row } from 'components/bootstrap';
import Routes from 'routing/Routes';
import QueryHelper from 'components/common/QueryHelper';

import styles from './EventDefinitions.css';
import EventDefinitionEntry from './EventDefinitionEntry';

import type { EventDefinition } from '../event-definitions-types';

const EmptyContent = () => (
  <Row>
    <Col md={6} mdOffset={3} lg={4} lgOffset={4}>
      <EmptyEntity>
        <p>
          Create Event Definitions that are able to search, aggregate or correlate Messages and other
          Events, allowing you to record significant Events in Graylog and alert on them.
        </p>
        <IfPermitted permissions="eventdefinitions:create">
          <LinkContainer to={Routes.ALERTS.DEFINITIONS.CREATE}>
            <Button bsStyle="success">Get Started!</Button>
          </LinkContainer>
        </IfPermitted>
      </EmptyEntity>
    </Col>
  </Row>
);

type Props = {
  eventDefinitions: Array<EventDefinition>,
  context?: React.ComponentProps<typeof EventDefinitionEntry>['context'],
  pagination: {
    grandTotal: number,
    page: number,
    pageSize: number,
    total: number,
  },
  query: string,
  onPageChange: (page: number, size: number) => void,
  onQueryChange: (newQuery?: string) => void,
  onDelete: (eventDefinition: EventDefinition) => void,
  onCopy: (eventDefinition: EventDefinition) => void,
  onEnable: (eventDefinition: EventDefinition) => void,
  onDisable: (eventDefinition: EventDefinition) => void,
};

export const PAGE_SIZES = [10, 50, 100];

const EventDefinitions = ({ eventDefinitions, context, pagination, query, onPageChange, onQueryChange, onDelete, onCopy, onEnable, onDisable }: Props) => {
  if (pagination.grandTotal === 0) {
    return <EmptyContent />;
  }

  const items = eventDefinitions.map((definition) => (
    <EventDefinitionEntry key={definition.id}
                          context={context}
                          eventDefinition={definition}
                          onDisable={onDisable}
                          onEnable={onEnable}
                          onDelete={onDelete}
                          onCopy={onCopy} />
  ));

  return (
    <Row>
      <Col md={12}>
        <SearchForm query={query}
                    onSearch={onQueryChange}
                    onReset={onQueryChange}
                    placeholder="Find Event Definitions"
                    wrapperClass={styles.inline}
                    queryHelpComponent={<QueryHelper entityName="event definition" />}
                    topMargin={0}
                    useLoadingState />

        <PaginatedList pageSizes={PAGE_SIZES}
                       totalItems={pagination.total}
                       onChange={onPageChange}>
          <div className={styles.definitionList}>
            <EntityList items={items} />
          </div>
        </PaginatedList>
      </Col>
    </Row>
  );
};

export default EventDefinitions;
