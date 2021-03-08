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
import { isEqual } from 'lodash';
import { useState, useEffect, useCallback, useMemo } from 'react';
import { useLocation, useHistory } from 'react-router-dom';
import PropTypes from 'prop-types';
import URI from 'urijs';

import { useStore } from 'stores/connect';
import useQuery from 'routing/useQuery';
import { WidgetStore } from 'views/stores/WidgetStore';
import WidgetFocusContext, { FocusRequest } from 'views/components/contexts/WidgetFocusContext';

const _updateQueryParams = ({
  focusedWidget,
  query,
} : {
  focusedWidget: FocusRequest,
  query: string,
}) => {
  let baseUri = new URI(query)
    .removeSearch('focusing')
    .removeSearch('editing')
    .removeSearch('focusedId');

  if (focusedWidget?.id && (focusedWidget.focusing || focusedWidget.editing)) {
    baseUri = baseUri.setSearch('focusedId', focusedWidget.id);
  }

  if (focusedWidget.focusing) {
    baseUri = baseUri.setSearch('focusing', true);
  }

  if (focusedWidget.editing) {
    baseUri = baseUri.setSearch('editing', true);
  }

  return baseUri.toString();
};

const useSyncStateWithQueryParams = ({ focusedWidget, focusUriParams, setFocusedWidget, widgets }) => {
  useEffect(() => {
    if ((focusedWidget?.id || focusUriParams?.id) && !isEqual(focusedWidget, focusUriParams)) {
      if (focusUriParams.id && !widgets.has(focusUriParams.id)) {
        return;
      }

      setFocusedWidget({
        id: focusUriParams.id,
        editing: focusUriParams.editing,
        focusing: focusUriParams.focusing ?? focusUriParams.editing,
      });
    }
  }, [focusedWidget, setFocusedWidget, widgets, focusUriParams]);
};

const WidgetFocusProvider = ({ children }: { children: React.ReactNode }): React.ReactElement => {
  const { search, pathname } = useLocation();
  const query = pathname + search;
  const history = useHistory();
  const [focusedWidget, setFocusedWidget] = useState<FocusRequest | undefined>();
  const widgets = useStore(WidgetStore);
  const params = useQuery();
  const focusUriParams = useMemo(() => ({
    editing: params.editing === 'true',
    focusing: params.focusing === 'true',
    id: params.focusedId,
  }), [params.editing, params.focusing, params.focusedId]);

  useSyncStateWithQueryParams({ focusedWidget, setFocusedWidget, widgets, focusUriParams });

  useEffect(() => {
    if (focusedWidget && !widgets.has(focusedWidget.id)) {
      setFocusedWidget(undefined);
    }
  }, [focusedWidget, widgets]);

  const updateFocusQueryParams = useCallback((newFocusedWidget: FocusRequest) => {
    const newURI = _updateQueryParams({
      focusedWidget: newFocusedWidget,
      query,
    });

    history.replace(newURI);
  }, [history, query]);

  const setWidgetFocusing = (widgetId: string | undefined) => {
    updateFocusQueryParams({
      id: focusedWidget?.id ?? widgetId,
      focusing: !!widgetId,
    });
  };

  const setWidgetEditing = (widgetId: string | undefined) => {
    updateFocusQueryParams({
      ...focusedWidget,
      id: focusedWidget?.id ?? widgetId,
      editing: !!widgetId,
    });
  };

  return (
    <WidgetFocusContext.Provider value={{ focusedWidget, setWidgetFocusing, setWidgetEditing }}>
      {children}
    </WidgetFocusContext.Provider>
  );
};

WidgetFocusProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export default WidgetFocusProvider;
