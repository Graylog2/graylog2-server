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

import { FormikInput, InputOptionalInfo } from 'components/common';
import GCSSetupInfo from 'components/gcs/GCSSetupInfo';
import type { GeoIpConfigType } from 'components/maps/configurations/types';

const GCSGeoIpFormGroup = () => {
  const { values } = useFormikContext<GeoIpConfigType>();

  return (
    <>
      <GCSSetupInfo />
      <FormikInput
        id="gcs_project_id"
        type="text"
        disabled={!values.enabled}
        label={
          <>
            Googe Cloud Storage Project ID <InputOptionalInfo />
          </>
        }
        name="gcs_project_id"
      />
    </>
  );
};

export default GCSGeoIpFormGroup;
