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
import { useContext } from 'react';

import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import WidgetContext from 'views/components/contexts/WidgetContext';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import { updateWidgetConfig } from 'views/logic/slices/widgetActions';
import type WidgetConfig from 'views/logic/widgets/WidgetConfig';
import WidgetColorContext from 'views/components/widgets/WidgetColorContext';
import WidgetErrorBoundary from 'views/components/widgets/WidgetErrorBoundary';
import WidgetVisualization from 'views/components/widgets/WidgetVisualization';

import { WidgetFromGroup } from './StandaloneWidgetGroup';
import useWidgetTitle from './useWidgetTitle';

const noop = () => {};

type VisualizationProps = {
  title: string;
  editing: boolean;
};

export const WidgetVisualizationFromContext = ({ title, editing }: VisualizationProps) => {
  const widget = useContext(WidgetContext);
  const fieldTypes = useContext(FieldTypesContext);
  const queryId = useActiveQueryId();
  const dispatch = useViewsDispatch();
  const onWidgetConfigChange = (newConfig: WidgetConfig) =>
    dispatch(updateWidgetConfig(widget.id, newConfig)).then(() => {});

  return (
    <WidgetErrorBoundary>
      <WidgetVisualization
        id={widget.id}
        widget={widget}
        title={title}
        editing={editing}
        queryId={queryId}
        fields={fieldTypes?.all}
        setLoadingState={noop}
        onToggleEdit={noop}
        onWidgetConfigChange={onWidgetConfigChange}
      />
    </WidgetErrorBoundary>
  );
};

const VisualizationWithTitle = () => {
  const widget = useContext(WidgetContext);
  const title = useWidgetTitle(widget);

  return <WidgetVisualizationFromContext title={title} editing={false} />;
};

type Props = {
  widgetId: string;
};

/**
 * Shows the results of a widget of the enclosing `StandaloneWidgetGroup`, without a frame.
 * `StandaloneWidgetCard` adds its title and time range.
 */
const StandaloneWidgetContent = ({ widgetId }: Props) => (
  <WidgetFromGroup widgetId={widgetId}>
    <WidgetColorContext id={widgetId}>
      <VisualizationWithTitle />
    </WidgetColorContext>
  </WidgetFromGroup>
);

export default StandaloneWidgetContent;
