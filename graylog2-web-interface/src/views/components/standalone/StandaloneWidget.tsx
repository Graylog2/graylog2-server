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
import type Widget from 'views/logic/widgets/Widget';
import WidgetColorContext from 'views/components/widgets/WidgetColorContext';
import WidgetErrorBoundary from 'views/components/widgets/WidgetErrorBoundary';
import WidgetVisualization from 'views/components/widgets/WidgetVisualization';

import StandaloneWidgetProvider from './StandaloneWidgetProvider';

const noop = () => {};

type VisualizationProps = {
  title: string;
  editing: boolean;
};

export const StandaloneWidgetVisualization = ({ title, editing }: VisualizationProps) => {
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

type Props = {
  widget: Widget;
  title: string;
};

/**
 * Shows a widget with its results outside of a search or dashboard.
 */
const StandaloneWidget = ({ widget, title }: Props) => (
  <StandaloneWidgetProvider widget={widget} title={title}>
    <WidgetColorContext id={widget.id}>
      <StandaloneWidgetVisualization title={title} editing={false} />
    </WidgetColorContext>
  </StandaloneWidgetProvider>
);

export default StandaloneWidget;
