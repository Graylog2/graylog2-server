import React from 'react';

import HiddenFieldWrapper from 'components/indices/IndexSetConfigurationForm/HiddenFieldWrapper';
import { FormikInput } from 'components/common';

const _validateIndexPrefix = (value: string) => {
  let error: string;

  if (value?.length === 0) {
    error = 'Invalid index prefix: cannot be empty';
  } else if (value?.indexOf('_') === 0 || value?.indexOf('-') === 0 || value?.indexOf('+') === 0) {
    error = 'Invalid index prefix: must start with a letter or number';
  } else if (value?.toLocaleLowerCase() !== value) {
    error = 'Invalid index prefix: must be lower case';
  } else if (!value?.match(/^[a-z0-9][a-z0-9_\-+]*$/)) {
    error = "Invalid index prefix: must only contain letters, numbers, '_', '-' and '+'";
  }

  return error;
};

const IndexSetReadOnlyConfiguration = ({
  hiddenFields,
  immutableFields,
  hasFieldRestrictionPermission,
}: {
  hiddenFields: string[];
  immutableFields: string[];
  hasFieldRestrictionPermission: boolean;
}) => {
  const indexPrefixHelp = (
    <span>
      A <strong>unique</strong> prefix used in Elasticsearch indices belonging to this index set. The prefix must start
      with a letter or number, and can only contain letters, numbers, &apos;_&apos;, &apos;-&apos; and &apos;+&apos;.
    </span>
  );

  return (
    <span>
      <HiddenFieldWrapper hiddenFields={hiddenFields} isPermitted={hasFieldRestrictionPermission}>
        <FormikInput
          type="text"
          id="index-prefix"
          label="Index prefix"
          name="index_prefix"
          help={indexPrefixHelp}
          validate={_validateIndexPrefix}
          required
          disabled={immutableFields.includes('index_prefix')}
        />
        <FormikInput
          type="text"
          id="index-analyzer"
          label="Analyzer"
          name="index_analyzer"
          help="Elasticsearch analyzer for this index set."
          required
          disabled={immutableFields.includes('index_analyzer')}
        />
      </HiddenFieldWrapper>
    </span>
  );
};

export default IndexSetReadOnlyConfiguration;
