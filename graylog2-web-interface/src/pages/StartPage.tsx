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
import React, { useCallback, useEffect } from 'react';

import { Spinner } from 'components/common';
import Routes from 'routing/Routes';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import { useStore } from 'stores/connect';
import useHistory from 'routing/useHistory';
import useActivePerspective from 'components/perspectives/hooks/useActivePerspective';

const StartPage = () => {
  const { currentUser } = useStore(CurrentUserStore);
  const { activePerspective } = useActivePerspective();
  const isLoading = !currentUser;
  const history = useHistory();

  const redirect = useCallback((page: string) => {
    history.replace(page);
  }, [history]);

  const redirectToStartPage = useCallback(() => {
    const startPage = currentUser?.startpage;

    // Show custom startpage if it was set
    if (startPage !== null && Object.keys(startPage).length > 0) {
      if (startPage.type === 'dashboard') {
        redirect(Routes.dashboard_show(startPage.id));
      } else if (startPage.type === 'stream') {
        redirect(Routes.stream_search(startPage.id));
      } else if (startPage.id !== 'default') {
        redirect(Routes.show_saved_search(startPage.id));
      } else {
        redirect(Routes.SEARCH);
      }

      return;
    }

    redirect(activePerspective.welcomeRoute);
  }, [activePerspective, currentUser?.startpage, redirect]);

  useEffect(() => {
    CurrentUserStore.reload();
  }, []);

  useEffect(() => {
    if (!isLoading) {
      redirectToStartPage();
    }
  }, [isLoading, redirectToStartPage]);

  return <Spinner />;
};

export default StartPage;
