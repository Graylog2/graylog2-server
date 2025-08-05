export type FieldData = {
  defaultValue?: any;
  value?: any;
  error?: string;
  dirty?: boolean;
  fileName?: string;
  fileContent?: string;
};

// This type is called FormDataType, because FormData is a reserved type
export type FormDataType = {
  [key: string]: FieldData;
};
