import React, { useContext } from "react";
import styled from "styled-components";

import { Input } from "components/bootstrap";

import AdditionalFields from 'integrations/aws/common/AdditionalFields';

import { FormDataContext } from '../common/context/FormData';
import { AdvancedOptionsContext } from '../common/context/AdvancedOptions';
import type { HandleFieldUpdateType, FormDataContextType, AdvancedOptionsContextType } from '../common/utils/types';

const StyledAdditionalFields = styled(AdditionalFields)`
  margin: 0 0 35px;
`;

interface Props {
  onChange: HandleFieldUpdateType;
}

const FormAdvancedOptions: React.FC<Props> = ({ onChange }) => {
  const { formData } = useContext<FormDataContextType>(FormDataContext);
  const { isAdvancedOptionsVisible, setAdvancedOptionsVisibility } =
    useContext<AdvancedOptionsContextType>(AdvancedOptionsContext);

  const { enableThrottling, overrideSource } = formData;

  const handleToggle: (visible: boolean) => void = (visible) => {
    setAdvancedOptionsVisibility(visible);
  };

  return (
    <StyledAdditionalFields
      title="Advanced Options"
      visible={isAdvancedOptionsVisible}
      onToggle={handleToggle}
    >

      <Input
        id="enableThrottling"
        type="checkbox"
        value="enable-throttling"
        defaultChecked={enableThrottling?.value}
        onChange={onChange}
        label="Enable Throttling"
        help="If enabled, no new messages will be read from this input until Graylog catches up with its message load. This is typically useful for inputs reading from files or message queue systems like AMQP or Kafka. If you regularly poll an external system, e.g. via HTTP, you normally want to leave this disabled."
      />

      <Input
        id="overrideSource"
        type="text"
        value={overrideSource?.value}
        onChange={onChange}
        label="Override Source (optional)"
        help="The message source is set to hostname|databaseName|tableName. If desired, you may override it with a custom value."
      />

    </StyledAdditionalFields>
  );
};

export default FormAdvancedOptions;