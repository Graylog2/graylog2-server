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
import { useTranslation } from 'react-i18next';
import { Formik, Form, useFormikContext } from 'formik';

import { PREFERENCES_THEME_MODE } from 'theme/constants';
import { Button, Row, Col, Input } from 'components/bootstrap';
import { FormikFormGroup, ReadOnlyFormGroup, Select } from 'components/common';
import SectionComponent from 'components/common/Section/SectionComponent';
import type User from 'logic/users/User';
import { saveUserPreferences, type PreferencesUpdateMap } from 'api/preferences';
import { DEFAULT_LANGUAGE, availableLanguages, getLanguage } from 'components/users/languages/Languages';
import LanguageLabel from 'components/users/languages/LanguageLabel';
import useProductName from 'brand-customization/useProductName';

const renderLanguageOption = ({ value }: { value: string }) => <LanguageLabel language={getLanguage(value)} showBeta />;

const LanguageSelect = () => {
  const { t } = useTranslation('users');
  const { values, setFieldValue } = useFormikContext<PreferencesUpdateMap>();
  const productName = useProductName();
  const options = availableLanguages().map(({ code, name }) => ({ value: code, label: name }));

  return (
    <Input
      id="language"
      labelClassName="col-sm-3"
      wrapperClassName="col-sm-9"
      label={t('user_details.preferences.language')}
      help={t('user_edit.preferences.language_help', { productName })}>
      <Select
        inputId="language"
        options={options}
        value={values.language ?? DEFAULT_LANGUAGE}
        onChange={(value) => setFieldValue('language', value)}
        optionRenderer={renderLanguageOption}
        valueRenderer={renderLanguageOption}
        clearable={false}
      />
    </Input>
  );
};

type Props = {
  user: User;
};

const PreferencesSection = ({ user }: Props) => {
  const { t } = useTranslation('users');
  const onSubmit = (data: PreferencesUpdateMap) => saveUserPreferences(user.username, data);

  return (
    <SectionComponent title={t('user_details.preferences.title')}>
      <Formik<PreferencesUpdateMap> onSubmit={onSubmit} initialValues={user.preferences}>
        {({ isSubmitting, isValid }) => (
          <Form className="form form-horizontal">
            <LanguageSelect />

            <Input
              id="timeout-controls"
              labelClassName="col-sm-3"
              wrapperClassName="col-sm-9"
              label={t('user_details.preferences.search_autocompletion')}>
              <FormikFormGroup
                label={t('user_edit.preferences.enable_autocompletion')}
                name="enableSmartSearch"
                formGroupClassName="form-group no-bm"
                type="checkbox"
              />
            </Input>

            <Input
              id="update-unfocused-controls"
              labelClassName="col-sm-3"
              wrapperClassName="col-sm-9"
              label={t('user_details.preferences.update_unfocused')}>
              <FormikFormGroup
                label={t('user_details.preferences.update_unfocused')}
                name="updateUnfocussed"
                formGroupClassName="form-group no-bm"
                type="checkbox"
              />
            </Input>

            <Input
              id="search-sidebar-controls"
              labelClassName="col-sm-3"
              wrapperClassName="col-sm-9"
              label={t('user_edit.preferences.search_sidebar')}>
              <FormikFormGroup
                label={t('user_edit.preferences.is_pinned')}
                name="searchSidebarIsPinned"
                formGroupClassName="form-group no-bm"
                type="checkbox"
                help={t('user_edit.preferences.search_sidebar_help')}
              />
            </Input>

            <Input
              id="dashboard-sidebar-controls"
              labelClassName="col-sm-3"
              wrapperClassName="col-sm-9"
              label={t('user_edit.preferences.dashboard_sidebar')}>
              <FormikFormGroup
                label={t('user_edit.preferences.is_pinned')}
                name="dashboardSidebarIsPinned"
                formGroupClassName="form-group no-bm"
                type="checkbox"
                help={t('user_edit.preferences.dashboard_sidebar_help')}
              />
            </Input>

            <ReadOnlyFormGroup
              label={t('user_details.preferences.theme_mode')}
              value={user.preferences?.[PREFERENCES_THEME_MODE] ?? t('user_details.preferences.not_configured')}
              help={t('user_edit.preferences.theme_mode_help')}
            />

            <Row className="no-bm">
              <Col xs={12}>
                <div className="pull-right">
                  <Button
                    bsStyle="primary"
                    disabled={isSubmitting || !isValid}
                    title={t('user_edit.preferences.update')}
                    type="submit">
                    {t('user_edit.preferences.update')}
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

export default PreferencesSection;
