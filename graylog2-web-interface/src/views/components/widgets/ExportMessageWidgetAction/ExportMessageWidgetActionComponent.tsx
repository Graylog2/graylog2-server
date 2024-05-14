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
import React, { useState } from 'react';

import ExportModal from 'views/components/export/ExportModal';
import type { WidgetMenuActionComponentProps } from 'views/components/widgets/Types';
import useView from 'views/hooks/useView';
import { IconButton } from 'components/common';

const ExportMessageWidgetActionComponent = ({ widget, disabled }: WidgetMenuActionComponentProps) => {
  const [showExport, setShowExport] = useState(false);
  const view = useView();
  const showMessageExportModal = () => setShowExport(true);

  return (
    <>
      <IconButton disabled={disabled} onClick={showMessageExportModal} name="download" title="Export all search results" />
      {showExport && (
      <ExportModal view={view}
                   directExportWidgetId={widget.id}
                   closeModal={() => setShowExport(false)} />
      )}
    </>
  );
};

export default ExportMessageWidgetActionComponent;
