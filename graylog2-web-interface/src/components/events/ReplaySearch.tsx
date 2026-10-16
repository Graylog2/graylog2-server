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
import { useMemo } from 'react';
import merge from 'lodash/merge';
import { useTranslation } from 'react-i18next';

import useCreateSearch from 'views/hooks/useCreateSearch';
import SearchPageLayoutProvider from 'views/components/contexts/SearchPageLayoutProvider';
import SearchPage from 'views/pages/SearchPage';
import type { LayoutState } from 'views/components/contexts/SearchPageLayoutContext';
import { isSystemEventDefinition } from 'components/event-definitions/event-definitions-types';
import Center from 'components/common/Center';
import type View from 'views/logic/views/View';

type ReplaySearchProps = {
  searchPageLayout?: Partial<LayoutState>;
  view: Promise<View>;
};

const defaultSearchPageLayout = {};

const ReplaySearch = ({ searchPageLayout = defaultSearchPageLayout, view: _view }: ReplaySearchProps) => {
  const { t } = useTranslation('events');
  const view = useCreateSearch(_view);

  const _searchPageLayout = useMemo(
    () =>
      merge(
        {
          sidebar: {
            isShown: true,
            title: t('alerts_replay.replayed_search'),
          },
        },
        searchPageLayout,
      ),
    [searchPageLayout, t],
  );

  return (
    <SearchPageLayoutProvider value={_searchPageLayout}>
      <SearchPage view={view} isNew forceSideBarPinned />
    </SearchPageLayoutProvider>
  );
};

export const LoadingBarrier = ({ children, eventDefinition }) => {
  const { t } = useTranslation('events');

  if (!isSystemEventDefinition(eventDefinition)) return children;

  return <Center>{t('alerts_replay.cannot_replay', { reason: t('alerts_replay.system_event') })}</Center>;
};

export default ReplaySearch;
