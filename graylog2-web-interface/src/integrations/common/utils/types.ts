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
