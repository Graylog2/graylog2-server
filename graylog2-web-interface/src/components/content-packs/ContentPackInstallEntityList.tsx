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

import Spinner from 'components/common/Spinner';
import { DataTable } from 'components/common';

import 'components/content-packs/ContentPackDetails.css';

type ContentPackInstallEntityListProps = {
  entities?: any[];
  uninstall?: boolean;
};

const ContentPackInstallEntityList = ({
  entities,
  uninstall = false,
}: ContentPackInstallEntityListProps) => {
  const rowFormatter = (entity) => (<tr><td>{entity.title}</td><td>{entity.type.name}</td></tr>);
  const headers = ['Title', 'Type'];
  const headerTitle = uninstall ? 'Entites to be uninstalled' : 'Installed Entities';

  if (!entities) {
    return <Spinner />;
  }

  return (
    <div>
      <h3>{headerTitle}</h3>
      <DataTable id="installed-entities"
                 headers={headers}
                 sortByKey="title"
                 dataRowFormatter={rowFormatter}
                 rows={entities}
                 filterKeys={[]} />
    </div>
  );
};

export default ContentPackInstallEntityList;
