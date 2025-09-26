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
import type { FormDataType, FieldData } from 'integrations/types';

export type AWSCloudTrailGenericInputCreateRequest = {
  type: 'org.graylog.aws.inputs.cloudtrail.CloudTrailInput';
  title: string;
  global: boolean;
  configuration: {
    polling_interval: number;
    throttling_allowed: boolean;
    cloudtrail_queue_name: string;
    aws_access_key: string;
    aws_secret_key: string;
    aws_region: string;
    assume_role_arn: string;
    override_source?: string;
  };
};

export type AWSCloudTrailInputCreateRequest = {
  name: string;
  enable_throttling: boolean;
  polling_interval: number;
  cloudtrail_queue_name: string;
  aws_access_key: string;
  aws_secret_key: string;
  aws_region: string;
  assume_role_arn: string;
  override_source?: string;
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
