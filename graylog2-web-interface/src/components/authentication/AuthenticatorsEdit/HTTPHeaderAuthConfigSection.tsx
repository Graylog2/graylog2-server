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
import { useState, useEffect } from 'react';
import { Formik, Form } from 'formik';
import { Trans, useTranslation } from 'react-i18next';

import Routes from 'routing/Routes';
import type HTTPHeaderAuthConfig from 'logic/authentication/HTTPHeaderAuthConfig';
import HTTPHeaderAuthConfigDomain from 'domainActions/authentication/HTTPHeaderAuthConfigDomain';
import { Input, Button, Col, Row, Alert } from 'components/bootstrap';
import { FormikFormGroup, ErrorAlert, Spinner } from 'components/common';
import SectionComponent from 'components/common/Section/SectionComponent';
import useHistory from 'routing/useHistory';
import type { HTTPHeaderAuthConfigJSON } from 'logic/authentication/HTTPHeaderAuthConfig';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useProductName from 'brand-customization/useProductName';

const HTTPHeaderAuthConfigSection = () => {
  const { t } = useTranslation('authentication');
  const [submitError, setSubmitError] = useState<string | undefined>();
  const productName = useProductName();

  const [loadedConfig, setLoadedConfig] = useState<HTTPHeaderAuthConfig | undefined | void>();
  const sectionTitle = t('trusted_header.title');
  const history = useHistory();
  const sendTelemetry = useSendTelemetry('authenticator-trustedheader');

  const _onSubmit = (data: HTTPHeaderAuthConfigJSON) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.AUTHENTICATION.CONFIG_UPDATED, {
      app_action_value: 'config-update',
    });

    setSubmitError(undefined);

    return HTTPHeaderAuthConfigDomain.update(data)
      .then(() => {
        history.push(Routes.SYSTEM.AUTHENTICATION.AUTHENTICATORS.SHOW);
      })
      .catch((error) => {
        setSubmitError(error.additional?.res?.text);
      });
  };

  useEffect(() => {
    HTTPHeaderAuthConfigDomain.load().then(setLoadedConfig);
  }, []);

  if (!loadedConfig) {
    return (
      <SectionComponent title={sectionTitle}>
        <Spinner />
      </SectionComponent>
    );
  }

  return (
    <SectionComponent title={sectionTitle}>
      <p>{t('trusted_header.description')}</p>
      <Formik onSubmit={_onSubmit} initialValues={loadedConfig.toJSON()}>
        {({ isSubmitting, isValid }) => (
          <Form className="form form-horizontal">
            <Input
              id="enable-http-header-auth"
              labelClassName="col-sm-3"
              wrapperClassName="col-sm-9"
              label={t('trusted_header.enabled')}>
              <FormikFormGroup
                label={t('trusted_header.enable_sso')}
                name="enabled"
                formGroupClassName="form-group no-bm"
                wrapperClassName="col-xs-12"
                type="checkbox"
              />
            </Input>
            <FormikFormGroup
              label={t('trusted_header.username_header')}
              name="username_header"
              required
              help={t('trusted_header.username_header_help')}
            />
            <Row>
              <Col mdOffset={3} md={9}>
                <Alert bsStyle="info">
                  <Trans
                    t={t}
                    i18nKey="trusted_header.trusted_proxies_hint"
                    values={{ productName }}
                    components={{ code: <code /> }}
                  />
                </Alert>
              </Col>
            </Row>
            <ErrorAlert runtimeError>{submitError}</ErrorAlert>
            <Row className="no-bm">
              <Col xs={12}>
                <div className="pull-right">
                  <Button
                    bsStyle="primary"
                    disabled={isSubmitting || !isValid}
                    title={t('trusted_header.update_config')}
                    type="submit">
                    {t('trusted_header.update_config')}
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

export default HTTPHeaderAuthConfigSection;
