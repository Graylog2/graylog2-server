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
import history from 'util/History';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';

import { useStore } from '../stores/connect';

const redirect = (page) => {
  history.replace(page);
};

const StartPage = () => {
  const { currentUser } = useStore(CurrentUserStore);
  const isLoading = !currentUser;
  const redirectToStartPage = useCallback(() => {
    const startPage = currentUser?.startpage;

    // Show custom startpage if it was set
    if (startPage !== null && Object.keys(startPage).length > 0) {
      if (startPage.type === 'stream') {
        redirect(Routes.stream_search(startPage.id));
      } else {
        redirect(Routes.dashboard_show(startPage.id));
      }

      return;
    }

    redirect(Routes.WELCOME);
  }, [currentUser?.startpage]);

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
