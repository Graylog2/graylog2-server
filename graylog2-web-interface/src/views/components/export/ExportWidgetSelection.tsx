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
import { useTranslation } from 'react-i18next';
import type { List } from 'immutable';

import type Widget from 'views/logic/widgets/Widget';
import type View from 'views/logic/views/View';
import { defaultCompare } from 'logic/DefaultCompare';
import { Row, Alert } from 'components/bootstrap';
import IfDashboard from 'views/components/dashboard/IfDashboard';
import IfSearch from 'views/components/search/IfSearch';
import Select from 'components/common/Select';

const sortOptions = (options) => options.sort((option1, option2) => defaultCompare(option1.label, option2.label));

type Props = {
  selectWidget: (widget: Widget) => void;
  widgets: List<Widget>;
  view: View;
};

const WidgetSelection = ({ selectWidget, widgets, view }: Props) => {
  const { t } = useTranslation('widgets');
  const widgetOption = (widget) => ({ label: view.getWidgetTitleByWidget(widget), value: widget });
  const widgetOptions = sortOptions(widgets.map((widget) => widgetOption(widget)).toArray());

  return (
    <>
      <Row>
        <IfSearch>
          {t('export.search_contains_all')}
          <br />
          {t('export.search_select_table')}
        </IfSearch>
        <IfDashboard>
          {t('export.dashboard_select_table')}
          <br />
          {t('export.dashboard_equals_action')}
        </IfDashboard>
      </Row>
      {widgets.size !== 0 ? (
        <Row>
          <label htmlFor="widget-selection">{t('export.select_message_table')}</label>
          <Select
            placeholder={t('export.select_message_table')}
            onChange={selectWidget}
            options={widgetOptions}
            inputId="widget-selection"
          />
        </Row>
      ) : (
        <Row>
          <Alert bsStyle="warning">{t('export.no_message_table')}</Alert>
        </Row>
      )}
    </>
  );
};

export default WidgetSelection;
