import * as React from 'react';
import { Field } from 'formik';

import usePluginEntities from 'views/logic/usePluginEntities';
import Select from 'components/common/Select';
import { defaultCompare } from 'views/logic/DefaultCompare';

type ExportFormat = {
  type: string,
  displayName: () => string,
  disabled?: () => boolean,
}

const ExportFormatSelection = () => {
  const exportFormats = usePluginEntities<ExportFormat>('views.export.formats');

  const exportFormatOptions = exportFormats.sort((type1, type2) => defaultCompare(type1?.displayName, type2?.displayName))
    .map(({ type, displayName, disabled = () => false }) => {
      const isDisabled = disabled();
      const title = displayName();

      return { label: title, value: type, disabled: isDisabled };
    });

  return (exportFormats.length > 1)
    ? (
      <Field name="format">
        {({ field: { name, value, onChange } }) => (
          <>
            <label htmlFor={name}>Output Format</label>
            <Select name={name} value={value} options={exportFormatOptions} onChange={(newFormat) => onChange({ target: { name, value: newFormat } })} />
          </>
        )}
      </Field>
    )
    : null;
};

export default ExportFormatSelection;
