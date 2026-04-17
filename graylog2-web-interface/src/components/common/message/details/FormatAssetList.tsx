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
import * as React from 'react';
import { useMemo } from 'react';

import usePluginEntities from 'hooks/usePluginEntities';

const FormatAssetList = ({ associated_assets }: { associated_assets: string[] }) => {
  const pluggableAssetListComponent = usePluginEntities('views.components.assetInformationActions');

  const assetsList = useMemo(
    () =>
      pluggableAssetListComponent.map(({ component: PluggableAssetListItem }) => (
        <PluggableAssetListItem key={associated_assets[0]} assetIds={associated_assets} direction="col" />
      )),
    [pluggableAssetListComponent, associated_assets],
  );

  if (associated_assets.length === 0) {
    return null;
  }

  return (
    <div>
      {assetsList.map((assetElement) => (
        <div key={assetElement.props.assetIds[0]}>{assetElement}</div>
      ))}
    </div>
  );
};

export default FormatAssetList;
