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
import * as React from 'react';
import styled from 'styled-components';
import { useCallback } from 'react';

import { TableHead, TableHeaderCell } from 'views/components/datatable';
import IfInteractive from 'views/components/dashboard/IfInteractive';
import type EventsWidgetConfig from 'views/logic/widgets/events/EventsWidgetConfig';
import EventsTableRow from 'views/components/widgets/events/EventsList/EventsTableRow';
import type { EventListItem } from 'views/components/widgets/events/types';
import EventsWidgetSortConfig from 'views/logic/widgets/events/EventsWidgetSortConfig';
import useEventAttributes from 'views/components/widgets/events/hooks/useEventAttributes';
import UnknownAttributeTitle from 'views/components/widgets/events/UnknownAttributeTitle';
import type Direction from 'views/logic/aggregationbuilder/Direction';

import AttributeSortIcon from '../../overview-configuration/AttributeSortIcon';

const TableWrapper = styled.div`
  overflow: auto;
`;

const ActionsHeader = styled(TableHeaderCell)`
  && {
    min-width: auto;
    width: 35px;
  }
`;

type Props = {
  config: EventsWidgetConfig,
  events: Array<EventListItem>,
  onSortChange: (sort: EventsWidgetSortConfig) => Promise<unknown>,
  setLoadingState: (loading: boolean) => void,
}

const EventsTable = ({ events, config, onSortChange, setLoadingState }: Props) => {
  const eventAttributes = useEventAttributes();
  const _onSortChange = useCallback((fieldName: string, nextDirection: Direction) => (
    onSortChange(new EventsWidgetSortConfig(fieldName, nextDirection))
  ), [onSortChange]);

  return (
    <TableWrapper>
      <table className="table table-condensed">
        <TableHead>
          <tr>
            {config.fields.toArray().map((field) => {
              const eventAttribute = eventAttributes.find(({ attribute }) => field === attribute);

              return (
                <TableHeaderCell key={field}>
                  {eventAttribute?.title ?? <UnknownAttributeTitle />}
                  {eventAttribute?.sortable && (
                    <AttributeSortIcon onSortChange={_onSortChange}
                                       attribute={field}
                                       attributeTitle={eventAttribute.title}
                                       activeAttribute={config.sort.field}
                                       activeDirection={config.sort.direction}
                                       setLoadingState={setLoadingState} />
                  )}
                </TableHeaderCell>
              );
            })}
            <IfInteractive>
              <ActionsHeader />
            </IfInteractive>
          </tr>
        </TableHead>
        <tbody>
          {events?.map((event) => (
            <EventsTableRow key={event.id}
                            event={event}
                            fields={config.fields} />
          ))}
        </tbody>
      </table>
    </TableWrapper>
  );
};

export default EventsTable;
