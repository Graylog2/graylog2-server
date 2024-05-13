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

import AggregationWidgetExportDropdown from 'views/components/widgets/ExportWidgetAction/AggregationWidgetExportDropdown';
import { supportedAggregationExportFormats } from 'views/Constants';
import { MenuItem } from 'components/bootstrap';
import type Widget from 'views/logic/widgets/Widget';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import useWidgetResults from 'views/components/useWidgetResults';
import type { Extension } from 'util/AggregationWidgetExportUtils';
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import { IconButton } from 'components/common';

type Props = {
  widget: Widget,
  onExportAggregationWidget: (extension: Extension) => void;
  showMessageExportModal: () => void;
}

const ExportWidgetAction = ({ widget, onExportAggregationWidget, showMessageExportModal }: Props) => {
  const { error: errors, widgetData: widgetResult } = useWidgetResults(widget.id);
  const showExportAggregationWidgetAction = widgetResult && widget.type === AggregationWidget.type && !errors?.length;
  const showExportMessageWidgetAction = widget.type === MessagesWidget.type && widget.isExportable;

  return (
    <>
      {showExportAggregationWidgetAction && (
        <AggregationWidgetExportDropdown>
          {
            supportedAggregationExportFormats.map(({ id: extension, title: extensionTitle }) => (
              <MenuItem key={extension} onSelect={() => onExportAggregationWidget(extension)}>
                {extensionTitle}
              </MenuItem>
            ))
          }
        </AggregationWidgetExportDropdown>
      )}
      {showExportMessageWidgetAction && (
        <IconButton onClick={showMessageExportModal} name="download" title="Export all search results" />
      )}
    </>
  );
};

export default ExportWidgetAction;
