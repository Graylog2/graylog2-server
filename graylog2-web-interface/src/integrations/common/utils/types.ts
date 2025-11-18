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
export type ErrorMessageType = {
  full_message: string;
  nice_message: React.ReactNode;
};

export interface FormFieldDataType {
  fileContent?: any;
  defaultValue?: any;
  value?: any;
  error?: string;
  dirty?: boolean;
}

export type HandleFieldUpdateType = (event: React.ChangeEvent<HTMLInputElement>, fieldData?: FormFieldDataType) => void;

export interface FormDataType {
  [key: string]: FormFieldDataType;
}

export interface FormDataContextType {
  formData: FormDataType;
  setFormData: (id: string, fieldData: FormFieldDataType) => void;
  clearField: (id: string) => void;
}

export type SidebarContextType = {
  sidebar: React.ReactElement;
  clearSidebar: () => void;
  setSidebar: React.Dispatch<React.SetStateAction<React.ReactElement>>;
};

export type isDisabledStepType = (step: string) => boolean;
export type SetEnabledStepType = (step: string) => void;
export type HandleSqsBatchSizeType = (step: string) => void;

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
