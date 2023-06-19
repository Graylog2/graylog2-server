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
import React, { useMemo, useEffect, useRef } from 'react';

import { Button } from 'components/bootstrap';
import usePluginEntities from 'hooks/usePluginEntities';

type Props = {
  onClose: () => void,
  value: string,
}

function usePrevious(value) {
  const ref = useRef();

  useEffect(() => {
    ref.current = value;
  }, [value]);

  return ref.current;
}

const ShowAssetInformation = ({ value, onClose }: Props) => {
  const pluggableAssetInformation = usePluginEntities('views.components.assetValueActions');

  const assetInformation = useMemo(() => pluggableAssetInformation.map(({ component: PluggableAssetInformation, key }) => (
    <PluggableAssetInformation key={`value-action-${key}`} identifier={value} />
  )), [pluggableAssetInformation, value]);

  if (value === usePrevious(value)) return null;

  const toggleShow = () => {
    onClose();
  };

  return (
    <div>
      <Button bsSize="xsmall" onClick={() => toggleShow()}>Close info</Button>
      {assetInformation}
    </div>
  );
};

export default ShowAssetInformation;
