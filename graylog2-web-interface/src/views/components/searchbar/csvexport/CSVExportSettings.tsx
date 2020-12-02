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
import { List } from 'immutable';

import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import Widget from 'views/logic/widgets/Widget';
import View from 'views/logic/views/View';
import { Input } from 'components/bootstrap';
import { Row } from 'components/graylog';
import FieldSelect from 'views/components/widgets/FieldSelect';
import IfDashboard from 'views/components/dashboard/IfDashboard';
import IfSearch from 'views/components/search/IfSearch';

type CSVExportSettingsType = {
  fields: List<FieldTypeMapping>,
  limit: number | undefined | null,
  setLimit: (limit: number) => void,
  selectedWidget: Widget | undefined | null,
  selectField: (fields: { label: string, value: string }[]) => void,
  selectedFields: { field: string }[] | undefined | null,
  view: View,
};

const SelectedWidgetInfo = ({ selectedWidget, view }: {selectedWidget: Widget, view: View}) => {
  const selectedWidgetTitle = view.getWidgetTitleByWidget(selectedWidget);

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
  selectedWidget,
  selectField,
  selectedFields,
  setLimit,
  limit,
  view,
}: CSVExportSettingsType) => {
  return (
    <>
      {selectedWidget && <SelectedWidgetInfo selectedWidget={selectedWidget} view={view} />}
      <Row>
        <p>
          Define the fields for your CSV file. You can change the field order with drag and drop.<br />
        </p>
        {selectedWidget && (
          <p>
            The export supports fields created by decorators which are part of the message table, but they currently do not appear in the field list. If you want to export a decorated field, just enter its name.
          </p>
        )}
        <p>
          When you&apos;ve finished the configuration, click on <q>Start Download</q>.
        </p>
      </Row>
      <Row>
        <label htmlFor="export-fields">Fields to export</label>
        <FieldSelect fields={fields} onChange={selectField} value={selectedFields} allowOptionCreation={!!selectedWidget} inputId="export-fields" />
      </Row>
      <Row>
        <label htmlFor="export-limit">Messages limit</label>
        <Input type="number"
               id="export-limit"
               onChange={({ target: { value } }) => setLimit(Number(value))}
               min={1}
               step={1}
               value={limit} />
      </Row>
      <Row>
        Messages are loaded in chunks. If a limit is defined, all chunks up to the one where the limit is reached will be retrieved. Which means the total number of delivered messages can be higher than the defined limit.
      </Row>
    </>
  );
};

export default CSVExportSettings;
