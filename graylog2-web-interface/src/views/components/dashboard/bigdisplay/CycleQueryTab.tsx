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
import { useEffect } from 'react';

import useAppDispatch from 'stores/useAppDispatch';
import { selectQuery } from 'views/logic/slices/viewSlice';
import useView from 'views/hooks/useView';
import useActiveQueryId from 'views/hooks/useActiveQueryId';

type Props = {
  interval: number,
  tabs?: Array<number> | undefined | null,
};

const CycleQueryTab = ({ interval, tabs }: Props) => {
  const view = useView();
  const activeQuery = useActiveQueryId();
  const dispatch = useAppDispatch();

  useEffect(() => {
    const cycleInterval = setInterval(() => {
      if (!view || !activeQuery) {
        return;
      }

      const queryTabs = tabs || view.search.queries.toIndexedSeq().map((_, v) => v).toJS();
      const currentQueryIndex = view.search.queries.toIndexedSeq().findIndex((q) => q.id === activeQuery);
      const currentTabIndex = queryTabs.indexOf(currentQueryIndex);
      const nextQueryIndex = queryTabs[(currentTabIndex + 1) % queryTabs.length];
      const nextQueryId = view.search.queries.toIndexedSeq().get(nextQueryIndex).id;

      if (nextQueryId !== activeQuery) {
        dispatch(selectQuery(nextQueryId));
      }
    }, interval * 1000);

    return () => clearInterval(cycleInterval);
  }, [interval, view, activeQuery, tabs, dispatch]);

  return null;
};

export default CycleQueryTab;
