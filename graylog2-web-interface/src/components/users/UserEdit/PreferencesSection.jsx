// @flow strict
import * as React from 'react';
import { Formik, Form } from 'formik';

import { PREFERENCES_THEME_MODE } from 'theme/constants';
import { Button, Row, Col } from 'components/graylog';
import { Input } from 'components/bootstrap';
import { FormikFormGroup, ReadOnlyFormGroup } from 'components/common';
import SectionComponent from 'components/common/Section/SectionComponent';
import User from 'logic/users/User';
import CombinedProvider from 'injection/CombinedProvider';

const { PreferencesActions } = CombinedProvider.get('Preferences');

type Props = {
  user: User,
};

const PreferencesSection = ({ user }: Props) => {
  const onSubmit = (data) => PreferencesActions.saveUserPreferences(user.username, data);

  return (
    <SectionComponent title="Preferences">
      <Formik onSubmit={onSubmit}
              initialValues={user.preferences}>
        {({ isSubmitting, isValid }) => (
          <Form className="form form-horizontal">
            <Input id="timeout-controls"
                   labelClassName="col-sm-3"
                   wrapperClassName="col-sm-9"
                   label="Search autocompletion">
              <FormikFormGroup label="Enable autocompletion"
                               name="enableSmartSearch"
                               formGroupClassName="form-group no-bm"
                               type="checkbox" />
            </Input>

            <Input id="update-unfocused-controls"
                   labelClassName="col-sm-3"
                   wrapperClassName="col-sm-9"
                   label="Update unfocused">
              <FormikFormGroup label="Update unfocused"
                               name="updateUnfocussed"
                               formGroupClassName="form-group no-bm"
                               type="checkbox" />
            </Input>

            <Input id="search-sidebar-controls"
                   labelClassName="col-sm-3"
                   wrapperClassName="col-sm-9"
                   label="Search sidebar">
              <FormikFormGroup label="Is pinned"
                               name="searchSidebarIsPinned"
                               formGroupClassName="form-group no-bm"
                               type="checkbox"
                               help="Can also be changed by using the search sidebar pin icon" />
            </Input>

            <Input id="dashboard-sidebar-controls"
                   labelClassName="col-sm-3"
                   wrapperClassName="col-sm-9"
                   label="Dashboard sidebar">
              <FormikFormGroup label="Is pinned"
                               name="dashboardSidebarIsPinned"
                               formGroupClassName="form-group no-bm"
                               type="checkbox"
                               help="Can also be changed by using the dashboard sidebar pin icon" />
            </Input>

            <ReadOnlyFormGroup label="Theme mode"
                               value={user.preferences?.[PREFERENCES_THEME_MODE] ?? 'Not configured'}
                               help="Can be changed by using the toggle in the user dropdown" />

            <Row className="no-bm">
              <Col xs={12}>
                <div className="pull-right">
                  <Button bsStyle="success"
                          disabled={isSubmitting || !isValid}
                          title="Update Preferences"
                          type="submit">
                    Update Preferences
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
