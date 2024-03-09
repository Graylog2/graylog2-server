import * as React from 'react';
import styled from 'styled-components';

import { IfPermitted } from 'components/common';
import { TableHead, TableHeaderCell } from 'views/components/datatable';
import IfInteractive from 'views/components/dashboard/IfInteractive';
import type EventsWidgetConfig from 'views/logic/widgets/events/EventsWidgetConfig';
import EventsTableRow from 'views/components/widgets/events/EventsList/EventsTableRow';
import { EVENT_ATTRIBUTES } from 'views/components/widgets/events/Constants';
import type { EventListItem } from 'views/components/widgets/events/types';
import type EventsWidgetSortConfig from 'views/logic/widgets/events/EventsWidgetSortConfig';

import AttributeSortIcon from './AttributeSortIcon';

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

const EventsTable = ({ events, config, onSortChange, setLoadingState }: Props) => (
  <TableWrapper>
    <table className="table table-condensed">
      <TableHead>
        <tr>
          {config.fields.toArray().map((field) => (
            <TableHeaderCell key={field}>
              {EVENT_ATTRIBUTES[field]?.title ?? field}
              <AttributeSortIcon onSortChange={onSortChange}
                                 field={field}
                                 fieldTitle={EVENT_ATTRIBUTES[field]?.title ?? field}
                                 activeSort={config.sort}
                                 setLoadingState={setLoadingState} />
            </TableHeaderCell>
          ))}
          <IfInteractive>
            <IfPermitted permissions="events:edit">
              <ActionsHeader />
            </IfPermitted>
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

export default EventsTable;
