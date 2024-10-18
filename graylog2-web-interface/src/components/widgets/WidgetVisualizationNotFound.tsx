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
import React, { useContext, useEffect } from 'react';

import { Alert } from 'components/bootstrap';
import RenderCompletionCallback from 'views/components/widgets/RenderCompletionCallback';

type Props = {
  widgetClassName: string,
};

const WidgetVisualizationNotFound = ({ widgetClassName }: Props) => {
  const onRenderComplete = useContext(RenderCompletionCallback);

  useEffect(() => onRenderComplete(), [onRenderComplete]);

  return (
    <Alert bsStyle="danger">
      Widget Visualization (<i>{widgetClassName}</i>) not found.
      It looks like the plugin supplying this widget is not loaded.
    </Alert>
  );
};

export default WidgetVisualizationNotFound;
