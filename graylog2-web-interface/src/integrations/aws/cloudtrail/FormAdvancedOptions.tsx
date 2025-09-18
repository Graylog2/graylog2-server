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
import React, { useContext } from 'react';

import ThrottlingCheckbox from 'integrations/components/ThrottlingCheckbox';
import { Input } from 'components/bootstrap';
import FormDataContext from 'integrations/contexts/FormDataContext';
import { AdvancedOptionsContext } from 'integrations/aws/context/AdvancedOptions';
import AdditionalFields from 'integrations/aws/common/AdditionalFields';

type FormAdvancedOptionsProps = {
  onChange: (...args: any[]) => void;
};

const FormAdvancedOptions = ({ onChange }: FormAdvancedOptionsProps) => {
  const { formData } = useContext(FormDataContext);
  const { isAdvancedOptionsVisible, setAdvancedOptionsVisibility } = useContext(AdvancedOptionsContext);

  const { overrideSource, awsCloudTrailThrottleEnabled } =
    formData;

  const handleToggle = (visible) => {
    setAdvancedOptionsVisibility(visible);
  };

  return (
    <AdditionalFields title="Advanced Options" visible={isAdvancedOptionsVisible} onToggle={handleToggle}>
      <ThrottlingCheckbox
        id="awsCloudTrailThrottleEnabled"
        defaultChecked={awsCloudTrailThrottleEnabled?.value}
        onChange={onChange}
      />

      <Input
        id="overrideSource"
        type="text"
        value={overrideSource?.value}
        onChange={onChange}
        label="Override Source (optional)"
        help="The source is set to aws-cloudtrail by default.If desired, you may override it with a custom value."
      />

    </AdditionalFields>
  );
};

export default FormAdvancedOptions;
