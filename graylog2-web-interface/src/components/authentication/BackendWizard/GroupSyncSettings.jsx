// @flow strict
import * as React from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';
import styled from 'styled-components';
import { Formik } from 'formik';

type Props = {
  formRef: React.ElementRef<typeof Formik | null>,
  onSubmitAll: () => void,
  validateOnMount: boolean,
};

const Header = styled.h4`
  margin-bottom: 5px;
`;

const NoEnterpriseComponent = () => (
  <>
    <Header>No enterprise plugin found</Header>
    <p>To use the <b>Teams</b> functionality you need to install the Graylog <b>Enterprise</b> plugin.</p>
  </>
);

const GroupSyncSettings = ({ onSubmitAll, formRef, validateOnMount }: Props) => {
  const authenticationPlugin = PluginStore.exports('authentication.groupSync');

  if (!authenticationPlugin || authenticationPlugin.length <= 0) {
    return <NoEnterpriseComponent />;
  }

  const { GroupSyncForm } = authenticationPlugin[0];

  return (
    <GroupSyncForm validateOnMount={validateOnMount}
                   formRef={formRef}
                   onSubmitAll={onSubmitAll} />
  );
};

export default GroupSyncSettings;
