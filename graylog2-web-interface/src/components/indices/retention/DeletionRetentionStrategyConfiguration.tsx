import * as React from 'react';

import { getValueFromInput } from 'util/FormsUtils';
import { Input } from 'components/bootstrap';
import { useIndexRetention } from 'components/indices/contexts/IndexRetentionContext';

type DeletionRetentionStrategyConfigurationProps = {
  updateConfig: (...args: any[]) => void;
};

const DeletionRetentionStrategyConfiguration = ({
  updateConfig
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
