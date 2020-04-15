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
