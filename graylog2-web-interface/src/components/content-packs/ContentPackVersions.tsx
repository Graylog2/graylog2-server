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

import { DataTable } from 'components/common';
import ContentPackVersionItem from 'components/content-packs/components/ContentPackVersionItem';
import type { ContentPackInstallation } from 'components/content-packs/Types';

import './ContentPackVersions.css';
import type ContentPackRevisions from 'logic/content-packs/ContentPackRevisions';

type Props = {
  contentPackRevisions: ContentPackRevisions,
  onDeletePack?: (id: string) => void
  onChange?: (id: string) => void
  onInstall?: (id: string, contentPackRev: string, parameters: unknown) => void
};

const headerFormatter = (header: React.ReactNode) => {
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
