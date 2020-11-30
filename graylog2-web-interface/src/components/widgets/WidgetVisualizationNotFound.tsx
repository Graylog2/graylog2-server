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
// @flow strict
import React, { useContext, useEffect } from 'react';
import PropTypes from 'prop-types';

import { Alert } from 'components/graylog';
import { Icon } from 'components/common';
import RenderCompletionCallback from 'views/components/widgets/RenderCompletionCallback';

type Props = {
  widgetClassName: string,
};

const WidgetVisualizationNotFound = ({ widgetClassName }: Props) => {
  const onRenderComplete = useContext(RenderCompletionCallback);

  useEffect(() => onRenderComplete(), [onRenderComplete]);

  return (
    <Alert bsStyle="danger">
      <Icon name="exclamation-circle" /> Widget Visualization (<i>{widgetClassName}</i>) not found.
      It looks like the plugin supplying this widget is not loaded.
    </Alert>
  );
};

WidgetVisualizationNotFound.propTypes = {
  widgetClassName: PropTypes.string.isRequired,
};

export default WidgetVisualizationNotFound;
