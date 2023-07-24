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

import usePluginEntities from 'hooks/usePluginEntities';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import type FieldType from 'views/logic/fieldtypes/FieldType';
import AddToQueryHandler from 'views/logic/valueactions/AddToQueryHandler';
import useAppDispatch from 'stores/useAppDispatch';
import type { AppDispatch } from 'stores/useAppDispatch';

const handleAddToQuery = (dispatch: AppDispatch, queryId: string, id: string, fieldType: FieldType) => {
  const field: string = 'associated_assets';

  return dispatch(AddToQueryHandler({ queryId, field, value: id, type: fieldType }));
};

const FormatAssetList = ({ associated_assets, fieldType }: { associated_assets: string[], fieldType: FieldType }) => {
  const pluggableAssetListComponent = usePluginEntities('views.components.assetInformationActions');
  const queryId = useActiveQueryId();
  const dispatch = useAppDispatch();

  const assetsList = React.useMemo(() => pluggableAssetListComponent.map(
    ({ component: PluggableAssetListItem }) => (
      <PluggableAssetListItem identifiers={associated_assets} addToQuery={(id) => handleAddToQuery(dispatch, queryId, id, fieldType)} />
    ),
  ), [pluggableAssetListComponent, associated_assets, dispatch, queryId, fieldType]);

  if (associated_assets.length === 0) {
    return null;
  }

  return (
    <div>
      <dt>Associated Assets</dt>
      {assetsList.map((assetElement) => (
        <div key={assetElement.props.identifiers[0]}>
          {assetElement}
        </div>
      ))}
    </div>
  );
};

export default FormatAssetList;
