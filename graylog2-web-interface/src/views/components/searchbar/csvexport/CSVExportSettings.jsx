// @flow strict
import React from 'react';
import { List, Map } from 'immutable';

import Direction from 'views/logic/aggregationbuilder/Direction';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import Widget from 'views/logic/widgets/Widget';

import { Input } from 'components/bootstrap';
import { Row } from 'components/graylog';
import FieldSelect from 'views/components/widgets/FieldSelect';
import FieldSortSelect from 'views/components/widgets/FieldSortSelect';
import IfDashboard from 'views/components/dashboard/IfDashboard';
import IfSearch from 'views/components/search/IfSearch';
import SortDirectionSelect from 'views/components/widgets/SortDirectionSelect';

type CSVExportSettingsType = {
  fields: List<FieldTypeMapping>,
  limit: ?number,
  setLimit: (limit: number) => void,
  selectedWidget: ?Widget,
  selectField: ({ label: string, value: string }[]) => void,
  selectedFields: ?{field: string}[],
  setSelectedSort: (Array<*>) => any,
  selectedSortDirection: Direction,
  selectedSort: SortConfig[],
  widgetTitles: Map<string, string>,
}

const SelectedWidgetInfo = ({ selectedWidget, widgetTitles }: {selectedWidget: Widget, widgetTitles: Map<string, string>}) => {
  const selectedWidgetTitle = widgetTitles.get(selectedWidget.id) || MessagesWidget.defaultTitle;
  return (
    <Row>
      <i>
        <IfSearch>
          {selectedWidget && `The following settings are based on the message table: ${selectedWidgetTitle}`}<br />
        </IfSearch>
        <IfDashboard>
          {selectedWidget && `You are currently exporting the search results for the message table: ${selectedWidgetTitle}`}<br />
        </IfDashboard>
      </i>
    </Row>
  );
};

const CSVExportSettings = ({
  fields,
  widgetTitles,
  selectedWidget,
  selectField,
  selectedFields,
  selectedSort,
  setSelectedSort,
  selectedSortDirection,
  limit,
  setLimit,
}: CSVExportSettingsType) => {
  const onSortDirectionChange = (newDirection) => {
    const newSort = selectedSort.map((sort) => sort.toBuilder().direction(newDirection).build());
    setSelectedSort(newSort);
  };
  return (
    <>
      {selectedWidget && <SelectedWidgetInfo selectedWidget={selectedWidget} widgetTitles={widgetTitles} />}
      <Row>
        Define the fields and sorting for your CSV file. You can change the field order with drag and drop.<br />
        When you&apos;ve finished the configuration, click on &quot;Start Download&quot;.
      </Row>
      <Row>
        <span>Select fields to export:</span>
        <FieldSelect fields={fields} onChange={selectField} value={selectedFields} />
      </Row>
      <Row>
        <span>Select sort (removing the sort will result in a faster export):</span>
        <FieldSortSelect fields={fields} sort={selectedSort} onChange={setSelectedSort} />
      </Row>
      <Row>
        <span>Select sort direction:</span>
        <SortDirectionSelect direction={selectedSortDirection ? selectedSortDirection.direction : null}
                             onChange={onSortDirectionChange} />
      </Row>
      <Row>
        <span>Appoximate message limit:</span>
        <Input type="number"
               id="export-message-limit"
               onChange={({ target: { value } }) => setLimit(Number(value))}
               value={limit} />
      </Row>
      <Row>
        There is no limit by default. Once the limit is reached, no further chunks will be exported. This means the limit does not equal the exact message amount.
      </Row>
    </>
  );
};

export default CSVExportSettings;
