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
  )
}

export default GCSGeoIpFormGroup
