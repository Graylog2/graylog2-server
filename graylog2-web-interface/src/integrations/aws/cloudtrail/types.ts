import type { FormDataType, FieldData } from 'integrations/types';

export type AWSCloudTrailGenericInputCreateRequest = {
  type: 'org.graylog.aws.inputs.cloudtrail.CloudTrailInput';
  title: string;
  global: boolean;
  configuration: {
    polling_interval: number;
    throttling_allowed: boolean;
    store_full_message: boolean;
    cloudtrail_queue_name: string;
    aws_access_key: string;
    aws_secret_key: string;
    aws_region: string;
    assume_role_arn: string;
  };
};

export type AWSCloudTrailInputCreateRequest = {
  name: string;
  enable_throttling: boolean;
  store_full_message: boolean;
  polling_interval: number;
  cloudtrail_queue_name: string;
  aws_access_key: string;
  aws_secret_key: string;
  aws_region: string;
  assume_role_arn: string;
};

export type ErrorMessageType = {
  full_message: string;
  nice_message: React.ReactNode;
};

export type HandleFieldUpdateType = (event: React.ChangeEvent<HTMLInputElement>, fieldData?: FieldData) => void;

export type SidebarContextType = {
  sidebar: React.ReactElement;
  clearSidebar: () => void;
  setSidebar: React.Dispatch<React.SetStateAction<React.ReactElement>>;
};

export type isDisabledStepType = (step: string) => boolean;
export type SetEnabledStepType = (step: string) => void;

export interface StepsContextType {
  availableSteps: string[];
  currentStep: string;
  enabledSteps: string[];
  isDisabledStep: isDisabledStepType;
  setAvailableStep: React.Dispatch<React.SetStateAction<string[]>>;
  setCurrentStep: React.Dispatch<React.SetStateAction<string>>;
  setEnabledStep: SetEnabledStepType;
}

export type AdvancedOptionsContextType = {
  isAdvancedOptionsVisible: boolean;
  setAdvancedOptionsVisibility: React.Dispatch<React.SetStateAction<boolean>>;
};

export type HandleSubmitType = (formData?: FormDataType) => void;

export type WizardStep = {
  key: string;
  title: JSX.Element;
  component: JSX.Element;
  disabled: boolean;
};
