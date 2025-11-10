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
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';

type Props = {
  displayPageLayout?: boolean;
};

const NotFoundPage = ({ displayPageLayout = true }: Props) => {
  const description = (
    <>
      <p>The page you are looking for does not exist (anymore).</p>
      <p>
        You can head back to the <Link to={Routes.WELCOME}>Main Page</Link> and navigate from there.
      </p>
    </>
  );

  return <ErrorPage title="Page not found" description={description} displayPageLayout={displayPageLayout} />;
};

export default NotFoundPage;
