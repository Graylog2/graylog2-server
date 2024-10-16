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
  uninstall = false
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
