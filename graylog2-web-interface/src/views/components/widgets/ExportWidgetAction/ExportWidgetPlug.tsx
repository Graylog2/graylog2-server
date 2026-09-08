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
import React, { useCallback } from 'react';

import { IconButton } from 'components/common';
import { MenuItem } from 'components/bootstrap';
import ActionDropdown from 'views/components/common/ActionDropdown';
import { usePngExportContext } from 'views/components/visualizations/PngExportContext';
import { createLinkAndDownload } from 'util/FileDownloadUtils';

const ExportWidgetPlug = () => {
  const { exportFn, widgetTitle } = usePngExportContext();

  const onExportAsPng = useCallback(() => {
    if (!exportFn) return;

    exportFn().then((dataUrl) => {
      const filename = widgetTitle ? `${widgetTitle}.png` : 'widget-export.png';
      createLinkAndDownload(dataUrl, filename);
    });
  }, [exportFn, widgetTitle]);

  const trigger = <IconButton name="download" title="Export widget" showTooltip={false} />;

  return (
    <ActionDropdown element={trigger} header="Export">
      <MenuItem disabled={!exportFn} onSelect={onExportAsPng}>
        PNG Image
      </MenuItem>
    </ActionDropdown>
  );
};

export default ExportWidgetPlug;
