import * as React from 'react';
import { Field } from 'formik';

import usePluginEntities from 'views/logic/usePluginEntities';
import Select from 'components/common/Select';

const ExportFormatSelection = () => {
  const exportFormats = usePluginEntities('views.export.formats');

  const exportFormatOptions = exportFormats.map(({ type, displayName }) => ({ label: displayName, value: type }));

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
