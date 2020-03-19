// @flow strict
import React from 'react';
import { List, Map } from 'immutable';

import Direction from 'views/logic/aggregationbuilder/Direction';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import Widget from 'views/logic/widgets/Widget';
import MessagesWidget from 'views/logic/widgets/MessagesWidget';

import { Row } from 'components/graylog';
import FieldSelect from 'views/components/widgets/FieldSelect';
import FieldSortSelect from 'views/components/widgets/FieldSortSelect';
import IfDashboard from 'views/components/dashboard/IfDashboard';
import IfSearch from 'views/components/search/IfSearch';
import SortDirectionSelect from 'views/components/widgets/SortDirectionSelect';

type ExportSettingsType = {
  fields: List<FieldTypeMapping>,
  selectedWidget: Widget,
  selectField: ({ label: string, value: string }[]) => void,
  selectedFields: ?{field: string}[],
  setSelectedSort: (Array<*>) => any,
  selectedSortDirection: Direction,
  selectedSort: SortConfig[],
  widgetTitles: Map<string, string>,
}

const ExportSettings = ({ fields, selectedWidget, selectField, selectedFields, setSelectedSort, selectedSortDirection, selectedSort, widgetTitles }: ExportSettingsType) => {
  const onSortDirectionChange = (newDirection) => {
    const newSort = selectedSort.map(sort => sort.toBuilder().direction(newDirection).build());
    setSelectedSort(newSort);
  };
  const selectedWidgetTitle = widgetTitles.get(selectedWidget.id) || MessagesWidget.defaultTitle;
  return (
    <>
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
      <Row>
        Define the fields and sorting for your CSV file. You can change the field order with drag and drop.<br />
        When you have finished the configuration, click on &quot;Start Download&quot;.
      </Row>
      <Row>
        <span>Select fields to export:</span>
        <FieldSelect fields={fields} onChange={selectField} value={selectedFields} />
      </Row>
      <Row>
        <span>Select sort:</span>
        <FieldSortSelect fields={fields} sort={selectedSort} onChange={setSelectedSort} />
      </Row>
      <Row>
        <span>Select sort direction:</span>
        <SortDirectionSelect direction={selectedSortDirection ? selectedSortDirection.direction : null}
                             onChange={onSortDirectionChange} />
      </Row>
    </>
  );
};

export default ExportSettings;
