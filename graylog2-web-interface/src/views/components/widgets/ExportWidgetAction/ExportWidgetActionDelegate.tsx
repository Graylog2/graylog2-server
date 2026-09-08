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

import { MenuItem } from 'components/bootstrap';
import type { WidgetMenuActionComponentProps } from 'views/components/widgets/Types';
import ExportWidgetPlug from 'views/components/widgets/ExportWidgetAction/ExportWidgetPlug';
import useWidgetExportActionComponent from 'views/components/widgets/useWidgetExportActionComponent';
import { usePngExportContext } from 'views/components/visualizations/PngExportContext';
import { createLinkAndDownload } from 'util/FileDownloadUtils';

const ExportWidgetActionDelegate = ({ widget, contexts, disabled }: WidgetMenuActionComponentProps) => {
  const ExportActionComponent = useWidgetExportActionComponent(widget);
  const { exportFn, widgetTitle } = usePngExportContext();

  const onExportAsPng = useCallback(() => {
    if (!exportFn) return;

    exportFn().then((dataUrl) => {
      const filename = widgetTitle ? `${widgetTitle}.png` : 'widget-export.png';
      createLinkAndDownload(dataUrl, filename);
    });
  }, [exportFn, widgetTitle]);

  const pngMenuItem = (
    <MenuItem disabled={!exportFn} onSelect={onExportAsPng}>
      PNG Image
    </MenuItem>
  );

  return !ExportActionComponent ? (
    <ExportWidgetPlug />
  ) : (
    <ExportActionComponent widget={widget} contexts={contexts} disabled={disabled} pngMenuItem={pngMenuItem} />
  );
};

export default ExportWidgetActionDelegate;
