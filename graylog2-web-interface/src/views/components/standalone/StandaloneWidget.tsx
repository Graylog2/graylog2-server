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
import type { SearchExecutors } from 'views/logic/slices/searchExecutionSlice';

import StandaloneWidgetGroup from './StandaloneWidgetGroup';
import StandaloneWidgetContent from './StandaloneWidgetContent';

type Props = {
  widget: Widget;
  title: string;
  /**
   * Renders the widget and runs its search only once it comes close to the viewport, e.g. for long lists of widgets.
   * Its container needs a height until then.
   */
  lazy?: boolean;
  /** Runs the widget's search, e.g. to show stored results instead. Must be stable across renders. */
  searchExecutors?: SearchExecutors;
};

/**
 * Shows a widget with its results outside of a search or dashboard. To run one search for several widgets,
 * use `StandaloneWidgetGroup` with `StandaloneWidgetCard` or `StandaloneWidgetContent` instead.
 */
const StandaloneWidget = ({ widget, title, lazy = false, searchExecutors = undefined }: Props) => (
  <StandaloneWidgetGroup
    widgets={[widget]}
    titles={{ [widget.id]: title }}
    lazy={lazy}
    searchExecutors={searchExecutors}>
    <StandaloneWidgetContent widgetId={widget.id} />
  </StandaloneWidgetGroup>
);

export default StandaloneWidget;
