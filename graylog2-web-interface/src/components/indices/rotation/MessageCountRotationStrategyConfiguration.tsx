import * as React from 'react';
import { useState } from 'react';

import { getValueFromInput } from 'util/FormsUtils';
import { Input } from 'components/bootstrap';

type MessageCountRotationStrategyConfigurationProps = {
  config: any;
  updateConfig: (...args: any[]) => void;
};

const MessageCountRotationStrategyConfiguration = ({
  config,
  updateConfig,
}: MessageCountRotationStrategyConfigurationProps) => {
  const { max_docs_per_index } = config;
  const [maxDocsPerIndex, setMaxDocsPerIndex] = useState(max_docs_per_index);

  const _onInputUpdate = (field) => (e) => {
    const update = {};
    const value = getValueFromInput(e.target);
    update[field] = value;

    setMaxDocsPerIndex(value);
    updateConfig(update);
  };

  return (
    <div>
      <Input type="number"
             id="max-docs-per-index"
             label="Max documents per index"
             onChange={_onInputUpdate('max_docs_per_index')}
             value={maxDocsPerIndex}
             help="Maximum number of documents in an index before it gets rotated"
             required />
    </div>
  );
};

export default MessageCountRotationStrategyConfiguration;
