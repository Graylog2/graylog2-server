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
import { useState, useEffect, useCallback } from 'react';
import { useLocation, useHistory } from 'react-router-dom';
import PropTypes from 'prop-types';
import URI from 'urijs';

import { useStore } from 'stores/connect';
import { WidgetStore } from 'views/stores/WidgetStore';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';

const _syncWithQuery = (query: string, focusedWidget: string) => {
  const baseUri = new URI(query)
    .removeSearch('focused');

  if (focusedWidget) {
    return baseUri.setSearch('focused', focusedWidget).toString();
  }

  return baseUri.toString();
};

const getFocusedWidgetFromSearch = (search: string) => {
  const uri = new URI(search);
  const { focused } = uri.search(true);

  return focused;
};

const WidgetFocusProvider = ({ children }: { children: React.ReactNode }): React.ReactElement => {
  const { search, pathname } = useLocation();
  const query = pathname + search;
  const history = useHistory();
  const [focusedWidget, setFocusedWidget] = useState<string | undefined>();
  const widgets = useStore(WidgetStore);

  useEffect(() => {
    const paramFocusedWidget = getFocusedWidgetFromSearch(search);

    if (paramFocusedWidget && focusedWidget !== paramFocusedWidget) {
      setFocusedWidget(paramFocusedWidget);
    }
  }, [focusedWidget, search]);

  useEffect(() => {
    if (focusedWidget && !widgets.has(focusedWidget)) {
      setFocusedWidget(undefined);
    }
  }, [focusedWidget, widgets]);

  const updateFocus = useCallback((widgetId: string | undefined | null) => {
    const newFocus = widgetId === focusedWidget
      ? undefined
      : widgetId;
    const newURI = _syncWithQuery(query, newFocus);
    history.replace(newURI);

    setFocusedWidget(newFocus);
  }, [focusedWidget, history, query]);

  return (
    <WidgetFocusContext.Provider value={{ focusedWidget, setFocusedWidget: updateFocus }}>
      {children}
    </WidgetFocusContext.Provider>
  );
};

WidgetFocusProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export default WidgetFocusProvider;
