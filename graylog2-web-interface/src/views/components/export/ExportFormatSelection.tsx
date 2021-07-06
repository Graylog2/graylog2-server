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
import { Field } from 'formik';

import usePluginEntities from 'views/logic/usePluginEntities';
import Select from 'components/common/Select';
import { defaultCompare } from 'views/logic/DefaultCompare';

const ExportFormatSelection = () => {
  const exportFormats = usePluginEntities('views.export.formats');

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
