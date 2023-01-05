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

import type View from 'views/logic/views/View';
import { ViewActions } from 'views/stores/ViewStore';

const useLoadView = (viewPromise: Promise<View>, queryId: string, isNew: boolean) => {
  useEffect(() => {
    viewPromise.then((view) => (isNew ? ViewActions.loadNew(view, queryId) : ViewActions.load(view, false, queryId)));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [viewPromise]);
};

export default useLoadView;
