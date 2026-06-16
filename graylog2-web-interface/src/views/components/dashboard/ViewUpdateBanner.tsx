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
import { useCallback } from 'react';
import styled from 'styled-components';

import { Alert, Button } from 'components/bootstrap';
import useView from 'views/hooks/useView';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import useViewsSelector from 'views/stores/useViewsSelector';
import { loadView } from 'views/logic/slices/viewSlice';
import { setServerViewLastUpdatedAt } from 'views/logic/slices/searchExecutionSlice';
import { selectServerViewLastUpdatedAt } from 'views/logic/slices/searchExecutionSelectors';
import { getView } from 'views/api/views';
import ViewDeserializer from 'views/logic/views/ViewDeserializer';

const BannerWrapper = styled.div`
  margin-bottom: 8px;
`;

const ViewUpdateBanner = () => {
  const view = useView();
  const dispatch = useViewsDispatch();
  const serverUpdatedAt = useViewsSelector(selectServerViewLastUpdatedAt);
  const isStale = serverUpdatedAt !== undefined;

  const viewId = view?.id;

  const dismiss = useCallback(() => dispatch(setServerViewLastUpdatedAt(undefined)), [dispatch]);

  const reload = useCallback(async () => {
    dismiss();
    const freshViewJson = await getView(viewId);
    const freshView = await ViewDeserializer(freshViewJson);
    dispatch(loadView(freshView));
  }, [dismiss, viewId, dispatch]);

  if (!isStale) return null;

  return (
    <BannerWrapper>
      <Alert bsStyle="info" onDismiss={dismiss}>
        This dashboard was updated by another user.{' '}
        <Button bsSize="xs" bsStyle="primary" onClick={reload}>Reload</Button>
      </Alert>
    </BannerWrapper>
  );
};

export default ViewUpdateBanner;
