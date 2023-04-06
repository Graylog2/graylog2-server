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
import React, { useEffect, useState } from 'react';
import { Formik, Form } from 'formik';

import SectionComponent from 'components/common/Section/SectionComponent';
import { FormikFormGroup, Spinner } from 'components/common';
import { Button, Row, Col, Input } from 'components/bootstrap';
import type { UserTelemetrySettings } from 'stores/telemetry/TelemetrySettingsStore';
import {
  TelemetrySettingsActions,
} from 'stores/telemetry/TelemetrySettingsStore';
import TelemetryInfoText from 'logic/telemetry/TelemetryInfoText';

const TelemetrySettingsConfig = () => {
  const [settings, setSettings] = useState<UserTelemetrySettings | undefined>(undefined);

  useEffect(() => {
    TelemetrySettingsActions.get().then((result) => {
      setSettings(result);
    });
  }, []);

  if (!settings) {
    return <Spinner />;
  }

  const onSubmit = (data: UserTelemetrySettings, { setSubmitting }) => {
    TelemetrySettingsActions.update(data).then(() => {
      setSubmitting(false);
    });
  };

  return (
    <SectionComponent title="Telemetry">
      <TelemetryInfoText />
      <Formik<UserTelemetrySettings> onSubmit={onSubmit}
                                     initialValues={settings}>
        {({ isSubmitting, isValid }) => (
          <Form className="form form-horizontal">
            <Input id="timeout-controls"
                   labelClassName="col-sm-3"
                   wrapperClassName="col-sm-9"
                   label="Enable telemetry">
              <FormikFormGroup label="enabled"
                               name="telemetry_enabled"
                               formGroupClassName="form-group no-bm"
                               type="checkbox" />
            </Input>

            <Row className="no-bm">
              <Col xs={12}>
                <div className="pull-right">
                  <Button bsStyle="success"
                          disabled={isSubmitting || !isValid}
                          title="Update Preferences"
                          type="submit">
                    Update telemetry
                  </Button>
                </div>
              </Col>
            </Row>
          </Form>
        )}
      </Formik>
    </SectionComponent>
  );
};

export default TelemetrySettingsConfig;
