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

import usePluginEntities from 'hooks/usePluginEntities';
import SecurityTeaserPage from 'components/security/teaser/SecurityTeaserPage';

const SecurityPageEntry = () => {
  const securityPagePlugins = usePluginEntities('securityPage');

  if (securityPagePlugins?.length) {
    // eslint-disable-next-line react/jsx-no-useless-fragment
    return <>{securityPagePlugins.map((SecurityPage, index) => <SecurityPage key={index} />)}</>;
  }

  return (
    <SecurityTeaserPage />
  );
};

export default SecurityPageEntry;
