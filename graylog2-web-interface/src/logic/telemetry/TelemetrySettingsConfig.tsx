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
import { usePostHog } from 'posthog-js/react';

import SectionComponent from 'components/common/Section/SectionComponent';
import { FormikFormGroup, Spinner } from 'components/common';
import { Button, Row, Col, Input } from 'components/bootstrap';
import type { UserTelemetrySettings } from 'stores/telemetry/TelemetrySettingsStore';
import {
  TelemetrySettingsActions,
} from 'stores/telemetry/TelemetrySettingsStore';
import AppConfig from 'util/AppConfig';
import TelemetryInfoText from 'logic/telemetry/TelemetryInfoText';

const TelemetrySettingsConfig = () => {
  const [settings, setSettings] = useState<UserTelemetrySettings | undefined>(undefined);
  const { enabled: isTelemetryEnabled } = AppConfig.telemetry() || {};
  const posthog = usePostHog();

  useEffect(() => {
    TelemetrySettingsActions.get().then((result) => {
      setSettings(result);
    });
  }, []);

  useEffect(() => {
    if (
      isTelemetryEnabled
      && settings?.telemetry_enabled
      && posthog?.has_opted_out_capturing()) {
      posthog.opt_in_capturing();
    }
  }, [isTelemetryEnabled, posthog, settings?.telemetry_enabled]);

  if (!settings) {
    return <Spinner />;
  }

  const updateTelemetryOpt = (data: UserTelemetrySettings) => {
    if (posthog && isTelemetryEnabled && !data.telemetry_enabled) {
      posthog.capture('$opt_out');
      posthog.opt_out_capturing();
    }
  };

  const onSubmit = (data: UserTelemetrySettings, { setSubmitting }) => {
    updateTelemetryOpt(data);

    TelemetrySettingsActions.update(data).then(() => {
      setSubmitting(false);
      window.location.reload();
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
                               disabled={!isTelemetryEnabled}
                               formGroupClassName="form-group no-bm"
                               type="checkbox" />
            </Input>

            <Row className="no-bm">
              <Col xs={12}>
                <div className="pull-right">
                  <Button bsStyle="success"
                          disabled={isSubmitting || !isValid || !isTelemetryEnabled}
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
