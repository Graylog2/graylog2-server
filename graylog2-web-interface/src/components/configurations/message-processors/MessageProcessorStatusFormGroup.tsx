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
import { useFormikContext } from 'formik';

import type { FormConfig } from 'components/configurations/message-processors/Types';
import { naturalSortIgnoreCase } from 'util/SortUtils';
import { isProcessorEnabled } from 'components/configurations/helpers';

const toggleProcessorStatus = (className: string, enabled: boolean, values: FormConfig) => {
  const disabledProcessors = values.disabled_processors;

  return enabled
    ? [...disabledProcessors, className]
    : disabledProcessors.filter((processorName) => processorName !== className);
};

const MessageProcessorStatusFormGroup = () => {
  const { values: formValues, setFieldValue } = useFormikContext<FormConfig>();
  const sortedProcessorOrder = [...formValues.processor_order].sort((a, b) => naturalSortIgnoreCase(a.name, b.name));

  return sortedProcessorOrder.map((processor) => {
    const enabled = isProcessorEnabled(processor, formValues);

    return (
      <tr key={processor.name}>
        <td>{processor.name}</td>
        {/* eslint-disable-next-line jsx-a11y/control-has-associated-label */}
        <td>
          <input
            type="checkbox"
            checked={enabled}
            onChange={() =>
              setFieldValue('disabled_processors', toggleProcessorStatus(processor.class_name, enabled, formValues))
            }
          />
        </td>
      </tr>
    );
  });
};

export default MessageProcessorStatusFormGroup;
