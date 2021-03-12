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

type QueryParamsUpdate = FocusRequest & { id: FocusRequest['id'] | undefined };

const _clearURI = (query) => new URI(query)
  .removeSearch('focusing')
  .removeSearch('editing')
  .removeSearch('focusedId');

const _updateQueryParams = ({
  newQueryParams,
  query,
} : {
  newQueryParams: QueryParamsUpdate,
  query: string,
}) => {
  let baseUri = _clearURI(query);

  if (newQueryParams?.id && (newQueryParams.focusing || newQueryParams.editing)) {
    baseUri = baseUri.setSearch('focusedId', newQueryParams.id);
  }

  if (newQueryParams.focusing) {
    baseUri = baseUri.setSearch('focusing', true);
  }

  if (newQueryParams.editing) {
    baseUri = baseUri.setSearch('editing', true);
  }

  return baseUri.toString();
};

const useSyncStateWithQueryParams = ({ focusedWidget, focusUriParams, setFocusedWidget, widgets }) => {
  useEffect(() => {
    const nextFocusedWidget = {
      id: focusUriParams.id,
      editing: focusUriParams.editing,
      focusing: focusUriParams.focusing || focusUriParams.editing,
    };

    if (!isEqual(focusedWidget, nextFocusedWidget)) {
      if (focusUriParams.id && !widgets.has(focusUriParams.id)) {
        return;
      }

      setFocusedWidget(nextFocusedWidget);
    }
  }, [focusedWidget, setFocusedWidget, widgets, focusUriParams]);
};

const useCleanupQueryParams = ({ focusUriParams, widgets, query, history }) => {
  useEffect(() => {
    if ((focusUriParams?.id || focusUriParams?.editing || focusUriParams?.focusing) && !widgets.has(focusUriParams.id)) {
      const baseURI = _clearURI(query);

      history.replace(baseURI.toString());
    }
  }, [focusUriParams, widgets, query, history]);
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

  useCleanupQueryParams({ focusUriParams, widgets, query, history });

  const updateFocusQueryParams = useCallback((newQueryParams: QueryParamsUpdate) => {
    const newURI = _updateQueryParams({
      newQueryParams: {
        ...newQueryParams,
        id: newQueryParams.id ?? focusedWidget?.id,
      },
      query,
    });

    history.replace(newURI);
  }, [history, query, focusedWidget]);

  const setWidgetFocusing = (widgetId: string | undefined) => {
    updateFocusQueryParams({
      id: widgetId,
      focusing: !!widgetId,
    });
  };

  const setWidgetEditing = (widgetId: string | undefined) => {
    updateFocusQueryParams({
      id: widgetId,
      editing: !!widgetId,
      focusing: focusUriParams.focusing,
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
