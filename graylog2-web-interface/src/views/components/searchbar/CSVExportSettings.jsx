// @flow strict
import React from 'react';
import { List } from 'immutable';

import Direction from 'views/logic/aggregationbuilder/Direction';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import Widget from 'views/logic/widgets/Widget';

import { Row } from 'components/graylog';
import FieldSelect from 'views/components/widgets/FieldSelect';
import FieldSortSelect from 'views/components/widgets/FieldSortSelect';
import IfDashboard from 'views/components/dashboard/IfDashboard';
import IfSearch from 'views/components/search/IfSearch';
import SortDirectionSelect from 'views/components/widgets/SortDirectionSelect';


type ExportSettingsType = {
  selectedWidget: Widget,
  selectField: ({ label: string, value: string }[]) => void,
  selectedFields: ?{field: string}[],
  setSelectedSort: (Array<*>) => any,
  selectedSortDirection: Direction,
  fields: List<FieldTypeMapping>,
  selectedSort: SortConfig[],
}

const ExportSettings = ({ selectedWidget, selectField, selectedFields, setSelectedSort, selectedSortDirection, fields, selectedSort }: ExportSettingsType) => {
  const onSortDirectionChange = (newDirection) => {
    const newSort = selectedSort.map(sort => sort.toBuilder().direction(newDirection).build());
    setSelectedSort(newSort);
  };
  return (
    <>
      <Row>
        Define the fields and sorting for your CSV file. You can change the field order with drag and drop.<br />
        When you have finished the configuration, click on <i>Start Download</i>.
      </Row>
      <Row>
        <IfSearch>
          {selectedWidget && `The settings got adopted from the message table: ${selectedWidget.id}`}<br />
        </IfSearch>
        <IfDashboard>
          {selectedWidget && `You are currently exporting the widget: ${selectedWidget.id}`}<br />
        </IfDashboard>

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
