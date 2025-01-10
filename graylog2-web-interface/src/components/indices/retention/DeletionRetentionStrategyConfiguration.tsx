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

import { getValueFromInput } from 'util/FormsUtils';
import { Input } from 'components/bootstrap';
import { useIndexRetention } from 'components/indices/contexts/IndexRetentionContext';

type DeletionRetentionStrategyConfigurationProps = {
  updateConfig: (...args: any[]) => void;
};

const DeletionRetentionStrategyConfiguration = ({
  updateConfig,
}: DeletionRetentionStrategyConfigurationProps) => {
  const [maxNumberOfIndices, setMaxNumberOfIndices] = useIndexRetention().useMaxNumberOfIndices;

  const _onInputUpdate = (field) => (e) => {
    const update = {};
    const value = getValueFromInput(e.target);
    update[field] = value;

    setMaxNumberOfIndices(value);
    updateConfig(update);
  };

  return (
    <div>
      <Input type="number"
             id="max-number-of-indices"
             label="Max number of indices"
             onChange={_onInputUpdate('max_number_of_indices')}
             value={maxNumberOfIndices}
             help={<span>Maximum number of indices to keep before <strong>deleting</strong> the oldest ones</span>}
             required />
    </div>
  );
};

export default DeletionRetentionStrategyConfiguration;
