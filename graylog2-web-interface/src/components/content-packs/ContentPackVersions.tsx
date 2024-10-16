import React from 'react';

import { DataTable } from 'components/common';
import ContentPackVersionItem from 'components/content-packs/components/ContentPackVersionItem';
import type { ContentPackVersionsType, ContentPackInstallation } from 'components/content-packs/Types';

import './ContentPackVersions.css';

type Props = {
  contentPackRevisions: ContentPackVersionsType,
  onDeletePack?: (id: string) => void
  onChange?: (id: string) => void
  onInstall?: (id: string, contentPackRev: string, parameters: unknown) => void
};

const headerFormatter = (header) => {
  if (header === 'Action') {
    return (<th className="text-right">{header}</th>);
  }

  return (<th>{header}</th>);
};

const ContentPackVersions = ({ onDeletePack = () => {}, contentPackRevisions, onInstall = () => {}, onChange = () => {} }: Props) => {
  const { contentPacks } = contentPackRevisions;
  const headers = ['Select', 'Revision', 'Action'];
  const rowFormatter = (item: ContentPackInstallation) => (
    <ContentPackVersionItem pack={item}
                            contentPackRevisions={contentPackRevisions}
                            onDeletePack={onDeletePack}
                            onChange={onChange}
                            onInstall={onInstall} />
  );

  return (
    <DataTable id="content-packs-versions"
               headers={headers}
               headerCellFormatter={headerFormatter}
               useNumericSort
               sortBy={(c) => c.rev.toString()}
               dataRowFormatter={rowFormatter}
               rows={contentPacks}
               filterKeys={[]} />
  );
};

export default ContentPackVersions;
