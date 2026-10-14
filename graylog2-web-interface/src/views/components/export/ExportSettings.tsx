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
import { Trans, useTranslation } from 'react-i18next';
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
  selectedWidget: Widget | undefined | null;
  view: View;
};

const SelectedWidgetInfo = ({ selectedWidget, view }: { selectedWidget: Widget; view: View }) => {
  const { t } = useTranslation('widgets');
  const selectedWidgetTitle = view.getWidgetTitleByWidget(selectedWidget);

  return (
    <Row>
      <i>
        <IfSearch>
          {selectedWidget && t('export.settings_based_on', { title: selectedWidgetTitle })}
          <br />
        </IfSearch>
        <IfDashboard>
          {selectedWidget && t('export.exporting_table', { title: selectedWidgetTitle })}
          <br />
        </IfDashboard>
      </i>
    </Row>
  );
};

const ExportSettings = ({ selectedWidget, view }: ExportSettingsType) => {
  const { t } = useTranslation('widgets');

  return (
    <>
      <Row>
        <ExportFormatSelection />
      </Row>

      {selectedWidget && <SelectedWidgetInfo selectedWidget={selectedWidget} view={view} />}
      <Row>
        <p>
          {t('export.define_fields')}
          <br />
        </p>
        {selectedWidget && <p>{t('export.decorated_fields_hint')}</p>}
        <p>
          <Trans t={t} i18nKey="export.finish_hint" components={{ quote: <q /> }} />
        </p>
      </Row>
      <Row>
        <Field name="selectedFields">
          {({ field: { name, value, onChange } }) => (
            <>
              <label htmlFor={name}>{t('export.fields_to_export')}</label>
              <FieldsConfiguration
                onChange={(newFields) =>
                  onChange({
                    target: { name, value: newFields.map((field) => ({ field })) },
                  })
                }
                selectSize="normal"
                displaySortableListOverlayInPortal
                selectedFields={value.map(({ field }) => field)}
                showSelectAllRest
                showDeSelectAll
                showListCollapseButton
              />
            </>
          )}
        </Field>
      </Row>
      <Row>
        <Field name="limit">
          {({ field: { name, value, onChange } }) => (
            <>
              <label htmlFor={name}>{t('export.messages_limit')}</label>
              <Input type="number" id={name} name={name} onChange={onChange} min={1} step={1} value={value} />
              <HelpBlock>{t('export.limit_help')}</HelpBlock>
            </>
          )}
        </Field>
      </Row>

      <CustomExportSettings widget={selectedWidget} />
    </>
  );
};

export default ExportSettings;
