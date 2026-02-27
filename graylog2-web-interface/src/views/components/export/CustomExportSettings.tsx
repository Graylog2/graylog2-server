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

import type Widget from 'views/logic/widgets/Widget';
import { widgetDefinition } from 'views/logic/Widgets';

type Props = {
  widget: Widget;
};

const CustomExportSettings = ({ widget }: Props) => {
  const { exportComponent: ExportComponent = () => null } = (widget?.type && widgetDefinition(widget.type)) ?? {};

  // eslint-disable-next-line react-hooks/static-components
  return <ExportComponent widget={widget} />;
};

export default CustomExportSettings;
