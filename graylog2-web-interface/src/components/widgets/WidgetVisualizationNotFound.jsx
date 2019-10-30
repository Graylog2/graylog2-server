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
