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
import { Field } from 'formik';

import type Widget from 'views/logic/widgets/Widget';
import type View from 'views/logic/views/View';
import { Input, HelpBlock, Row } from 'components/bootstrap';
import IfDashboard from 'views/components/dashboard/IfDashboard';
import IfSearch from 'views/components/search/IfSearch';
import ExportFormatSelection from 'views/components/export/ExportFormatSelection';
import FieldsConfiguration from 'views/components/widgets/FieldsConfiguration';

import CustomExportSettings from './CustomExportSettings';

type ExportSettingsType = {
  selectedWidget: Widget | undefined | null,
  view: View,
};

const SelectedWidgetInfo = ({ selectedWidget, view }: { selectedWidget: Widget, view: View }) => {
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

const ExportSettings = ({
  selectedWidget,
  view,
}: ExportSettingsType) => (
  <>
    <Row>
      <ExportFormatSelection />
    </Row>

    {selectedWidget && <SelectedWidgetInfo selectedWidget={selectedWidget} view={view} />}
    <Row>
      <p>
        Define the fields for your file.<br />
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
      <Field name="selectedFields">
        {({ field: { name, value, onChange } }) => (
          <>
            <label htmlFor={name}>Fields to export</label>
            <FieldsConfiguration onChange={
                                      (newFields) => onChange({
                                        target: { name, value: newFields.map((field) => ({ field })) },
                                      })
                                   }
                                 selectSize="normal"
                                 displaySortableListOverlayInPortal
                                 selectedFields={value.map(({ field }) => field)}
                                 showSelectAllRest
                                 showDeSelectAll
                                 showListCollapseButton />
          </>
        )}
      </Field>
    </Row>
    <Row>
      <Field name="limit">
        {({ field: { name, value, onChange } }) => (
          <>
            <label htmlFor={name}>Messages limit</label>
            <Input type="number"
                   id={name}
                   name={name}
                   onChange={onChange}
                   min={1}
                   step={1}
                   value={value} />
            <HelpBlock>
              Messages are loaded in chunks. If a limit is defined, all chunks up to the one where the limit is reached will be retrieved. Which means the total number of delivered messages can be higher than the defined limit.
            </HelpBlock>
          </>
        )}
      </Field>
    </Row>

    <CustomExportSettings widget={selectedWidget} />
  </>
);

export default ExportSettings;
