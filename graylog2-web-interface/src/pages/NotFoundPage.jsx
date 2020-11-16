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
import React from 'react';

import ErrorPage from 'components/errors/ErrorPage';

const NotFoundPage = () => {
  const description = (
    <>
      <p>The party gorilla was just here, but had another party to rock.</p>
      <p>Oh, party gorilla! How we miss you! Will we ever see you again?</p>
    </>
  );

  return (<ErrorPage title="Page not found" description={description} />);
};

export default NotFoundPage;
