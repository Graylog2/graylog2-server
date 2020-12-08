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
import PropTypes from 'prop-types';
import React from 'react';

import Spinner from 'components/common/Spinner';
import { DataTable } from 'components/common';

import 'components/content-packs/ContentPackDetails.css';

const ContentPackInstallEntityList = (props) => {
  const rowFormatter = (entity) => (<tr><td>{entity.title}</td><td>{entity.type.name}</td></tr>);
  const headers = ['Title', 'Type'];
  const headerTitle = props.uninstall ? 'Entites to be uninstalled' : 'Installed Entities';

  if (!props.entities) {
    return <Spinner />;
  }

  return (
    <div>
      <h3>{headerTitle}</h3>
      <DataTable id="installed-entities"
                 headers={headers}
                 sortByKey="title"
                 dataRowFormatter={rowFormatter}
                 rows={props.entities}
                 filterKeys={[]} />
    </div>
  );
};

ContentPackInstallEntityList.propTypes = {
  entities: PropTypes.array,
  uninstall: PropTypes.bool,
};

ContentPackInstallEntityList.defaultProps = {
  entities: undefined,
  uninstall: false,
};

export default ContentPackInstallEntityList;
