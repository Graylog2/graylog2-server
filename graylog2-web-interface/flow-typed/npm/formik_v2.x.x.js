// flow-typed signature: 6f29933b3ab5a0d396b124bdf34ef768
// flow-typed version: 5c17533077/formik_v2.x.x/flow_>=v0.104.x

declare module 'formik/@@yup' {
  declare export type Schema = any;
  declare export type YupError = any;
}

declare module 'formik/@flow-typed' {
  import type { Schema } from 'formik/@@yup';

  declare export type FieldValidator = (
    value: any
  ) => ?string | Promise<?string>;
  declare export type FormikErrors<Values> = $ObjMap<Values, () => ?string>;
  declare export type FormikTouched<Values> = $ObjMap<Values, () => ?boolean>;

  declare export type FormikState<Values> = {|
    values: Values,
    errors: FormikErrors<Values>,
    touched: FormikTouched<Values>,
    isSubmitting: boolean,
    isValidating: boolean,
    submitCount: number,
    status?: any,
  |};

  declare export type FormikComputedProps<Values> = {|
    dirty: boolean,
    isValid: boolean,
    initialValues: Values,
    initialErrors: FormikErrors<Values>,
    initialTouched: FormikTouched<Values>,
    initialStatus?: any,
  |};

  declare export type FormikHelpers<Values> = {|
    setStatus(status?: any): void,
    setErrors(errors: FormikErrors<Values>): void,
    setSubmitting(isSubmitting: boolean): void,
    setTouched(touched: FormikTouched<Values>): void,
    setValues(values: Values): void,
    setFieldValue(fieldName: string, value: any, shouldValidate?: boolean): void,
    setFieldError(fieldName: $Keys<Values>, message: string): void,
    setFieldTouched(fieldName: $Keys<Values>, isTouched?: boolean, shouldValidate?: boolean): void,
    validateForm(values?: $Shape<Values>): Promise<FormikErrors<Values>>,
    validateField(field: string): void,
    resetForm(nextState?: $Shape<FormikState<Values>>): void,
    setFormikState(
      state:
        | FormikState<Values>
        | ((prevState: FormikState<Values>) => FormikState<Values>),
      callback?: () => void
    ): void,
  |};

  declare export type FormikHandlers = {|
    handleSubmit(e?: {...}): void,
    handleReset(e?: {...}): void,
    handleBlur(e: {...}): void,
    handleChange(e: {...}): void,
  |};

  declare export type FormikSharedConfig = {|
    validateOnChange?: boolean,
    validateOnBlur?: boolean,
    isInitialValid?: boolean | ((props: {...}) => boolean),
    enableReinitialize?: boolean,
  |};

  declare export type FormikConfig<Values> = {|
    ...FormikSharedConfig,
    onSubmit: (values: Values, formikHelpers: FormikHelpers<Values>) => void,

    component?: React$ComponentType<FormikProps<Values>> | React$Node,
    render?: (props: FormikProps<Values>) => React$Node,
    children?: ((props: FormikProps<Values>) => React$Node) | React$Node,
    initialValues?: $Shape<Values>,
    initialStatus?: any,
    initialErrors?: FormikErrors<Values>,
    initialTouched?: FormikTouched<Values>,
    onReset?: (values: Values, formikHelpers: FormikHelpers<Values>) => void,
    validationSchema?: (() => Schema) | Schema,
    validate?: (values: Values) => void | {...} | Promise<FormikErrors<Values>>,
  |};

  declare export type FormikProps<Values> = $ReadOnly<{|
    ...FormikSharedConfig,
    ...FormikState<Values>,
    ...FormikHelpers<Values>,
    ...FormikHandlers,
    ...FormikComputedProps<Values>,
    ...FormikRegistration<Values>,
    submitForm: () => Promise<void>,
  |}>;

  declare export type FnsOptions = {|
    validate: FieldValidator,
  |};

  declare export type FormikRegistration<Values> = {|
    unregisterField(fieldName: $Keys<Values>): void,
    registerField(fieldName: $Keys<Values>, fns: FnsOptions): void,
  |};

  declare export type FormikContext<Values> = FormikProps<Values> & {
    validate: $ElementType<FormikConfig<Values>, 'validate'>,
    validationSchema: $ElementType<FormikConfig<Values>, 'validationSchema'>,
    ...
  };

  declare export interface SharedRenderProps<T> {
    component?: React$ElementType;
    render?: (props: T) => React$Node;
    children?: (props: T) => React$Node;
  }

  declare export type FieldMetaProps<Value> = $ReadOnly<{|
    value: Value,
    error?: string,
    touched: boolean,
    initialValue?: Value,
    initialTouched: boolean,
    initialError?: string,
  |}>;

  declare export type FieldInputProps<Value> = $ReadOnly<{|
    value: Value,
    name: string,
    multiple?: boolean,
    checked?: boolean,
    onChange: $ElementType<FormikHandlers, 'handleChange'>,
    onBlur: $ElementType<FormikHandlers, 'handleBlur'>,
  |}>;
}

declare module 'formik/@withFormik' {
  import type { Schema } from 'formik/@@yup';
  import type {
    FormikHelpers,
    FormikProps,
    FormikSharedConfig,
    FormikTouched,
    FormikErrors,
  } from 'formik/@flow-typed';

  declare export type InjectedFormikProps<Props, Values> = $ReadOnly<{|
    ...FormikProps<Values>,
    ...$Exact<Props>,
  |}>;

  declare export type FormikBag<Props, Values> = $ReadOnly<{|
    ...FormikHelpers<Values>,
    props: Props,
  |}>;

  declare export type WithFormikConfig<Props, Values> = {|
    ...FormikSharedConfig,

    handleSubmit: (values: Values, formikBag: FormikBag<Props, Values>) => void,

    displayName?: string,
    mapPropsToValues?: (props: Props) => $Shape<Values>,
    mapPropsToStatus?: (props: Props) => any,
    mapPropsToTouched?: (props: Props) => FormikTouched<Values>,
    mapPropsToErrors?: (props: Props) => FormikErrors<Values>,
    validate?: (values: Values, props: Props) => void | {...} | Promise<any>,

    validationSchema?: ((props: Props) => Schema) | Schema,
  |};

  declare export function withFormik<Props: {...}, Values: {...}>(
    options: WithFormikConfig<Props, Values>
  ): (
    component: React$ComponentType<InjectedFormikProps<Props, Values>>
  ) => React$ComponentType<Props>;
}

declare module 'formik/@Field' {
  import type {
    FormikProps,
    FieldMetaProps,
    FieldInputProps,
    FieldValidator
  } from 'formik/@flow-typed';

  declare export type FieldProps<Value> = {|
    field: FieldInputProps<Value>,
    form: FormikProps<Value>,
    meta: FieldMetaProps<Value>,
  |};

  declare export type FieldConfig<Value> = {|
    name: string,
    component?: React$ElementType,
    as?: React$ElementType,
    render?: (props: FieldProps<Value>) => React$Node,
    children?: ((props: FieldProps<Value>) => React$Node) | React$Node,
    validate?: FieldValidator,
    type?: string,
    value?: Value,
    innerRef?: React$Ref<any>,
  |};

  declare export type FieldAttributes<Props, Value> = { ...FieldConfig<Value>, ... } & Props;

  declare export type UseFieldConfig<Value> = {
    name: string,
    type?: string,
    value?: Value,
    as?: React$ElementType,
    multiple?: boolean,
    ...
  };

  declare export function useField<Value>(
    propsOrFieldName: string | UseFieldConfig<Value>
  ): [FieldInputProps<Value>, FieldMetaProps<Value>];

  declare export var Field: { <Props, Value>(props: FieldAttributes<Props, Value>): React$Node, ... };

  declare export var FastField: typeof Field;
}

declare module 'formik/@utils' {
  declare export function isFunction(value: any): boolean;
  declare export function isObject(value: any): boolean;
  declare export function isInteger(value: any): boolean;
  declare export function isString(value: any): boolean;
  declare export function isNaN(value: any): boolean;
  declare export function isEmptyChildren(value: any): boolean;
  declare export function isPromise(value: any): boolean;
  declare export function isInputEvent(value: any): boolean;
  declare export function getActiveElement(doc?: Document): Element | null;
  declare export function getIn(
    obj: any,
    key: string | Array<string>,
    def?: any,
    p?: number
  ): any;
  declare export function setIn(obj: any, path: string, value: any): any;
  declare export function setNestedObjectValues<T>(
    object: any,
    value: any,
    visited?: any,
    response?: any
  ): T;
}

declare module 'formik/@FormikContext' {
  import type { FormikContext } from 'formik/@flow-typed';

  declare type _Context = React$Context<FormikContext<{...}>>;

  declare export var FormikProvider: $ElementType<_Context, 'Provider'>;
  declare export var FormikConsumer: $ElementType<_Context, 'Consumer'>;

  declare export function useFormikContext<Values>(): FormikContext<Values>;
}

declare module 'formik/@ErrorMessage' {
  declare export type ErrorMessageProps = {
    name: string,
    className?: string,
    component?: React$ElementType,
    render?: (errorMessage: string) => React$Node,
    children?: (errorMessage: string) => React$Node,
    ...
  };

  declare export var ErrorMessage: React$ComponentType<ErrorMessageProps>;
}

declare module 'formik/@FieldArray' {
  import type { SharedRenderProps, FormikProps } from 'formik/@flow-typed';

  declare export type FieldArrayRenderProps<Values> = ArrayHelpers & {
    form: FormikProps<Values>,
    name: string,
    ...
  };

  declare export type FieldArrayConfig<Values> = {
    name: string,
    validateOnChange?: boolean,
    ...
  } & SharedRenderProps<FieldArrayRenderProps<Values>>;

  declare export type ArrayHelpers = {
    push: (obj: any) => void,
    handlePush: (obj: any) => () => void,
    swap: (indexA: number, indexB: number) => void,
    handleSwap: (indexA: number, indexB: number) => () => void,
    move: (from: number, to: number) => void,
    handleMove: (from: number, to: number) => () => void,
    insert: (index: number, value: any) => void,
    handleInsert: (index: number, value: any) => () => void,
    replace: (index: number, value: any) => void,
    handleReplace: (index: number, value: any) => () => void,
    unshift: (value: any) => number,
    handleUnshift: (value: any) => () => void,
    handleRemove: (index: number) => () => void,
    handlePop: () => () => void,
    remove<T>(index: number): ?T,
    pop<T>(): ?T,
    ...
  };

  declare export function move<T>(
    array: Array<T>,
    from: number,
    to: number
  ): Array<T>;
  declare export function swap<T>(
    array: Array<T>,
    indexA: number,
    indexB: number
  ): Array<T>;
  declare export function insert<T>(
    array: Array<T>,
    index: number,
    value: T
  ): Array<T>;
  declare export function replace<T>(
    array: Array<T>,
    index: number,
    value: T
  ): Array<T>;

  declare export var FieldArray: { <Values>(props: FieldArrayConfig<Values>): React$Node, ... };
}

declare module 'formik/@Form' {
  declare export type HTMLFormAttributes = {
    // `onSubmit` and `onReset` are not overwritable props
    // https://github.com/jaredpalmer/formik/blob/next/docs/api/form.md
    onSubmit?: empty,
    onReset?: empty,
    ...
  };

  declare export var Form: React$StatelessFunctionalComponent<HTMLFormAttributes>;
}

declare module 'formik/@Formik' {
  import type { UseFieldConfig } from 'formik/@Field';
  import type { YupError, Schema } from 'formik/@@yup';
  import type {
    FormikConfig,
    FormikErrors,
    FormikState,
    FormikTouched,
    FieldMetaProps,
    FieldInputProps,
  } from 'formik/@flow-typed';

  declare export function useFormik<Values>(
    options: FormikConfig<Values>
  ): {
    initialValues: $Shape<Values>,
    initialErrors: FormikErrors<Values>,
    initialTouched: FormikTouched<Values>,
    initialStatus: any,
    handleBlur(fieldName: $Keys<Values>): (event: {...}) => void,
    handleBlur(event: {...}): void,
    handleChange(fieldName: $Keys<Values>): (event: {...}) => void,
    handleChange(event: {...}): void,
    handleReset: (e?: {...}) => void,
    handleSubmit: (e?: {...}) => void,
    resetForm: (nextState?: $Shape<FormikState<Values>>) => void,
    setErrors: (errors: FormikErrors<Values>) => void,
    setFormikState: (
      stateOrCb:
        | FormikState<Values>
        | ((state: FormikState<Values>) => FormikState<Values>)
    ) => void,
    setFieldTouched: (
      fieldName: $Keys<Values>,
      touched?: boolean,
      shouldValidate?: boolean
    ) => void,
    setFieldValue: <Name: $Keys<Values>>(
      fieldName: Name,
      value: $ElementType<Values, Name>,
      shouldValidate?: boolean
    ) => void,
    setFieldError: (fieldName: $Keys<Values>, value: ?string) => void,
    setStatus: (status: any) => void,
    setSubmitting: (isSubmitting: boolean) => void,
    setTouched: (touched: FormikTouched<Values>) => void,
    setValues: (values: $Shape<Values>) => void,
    submitForm: () => Promise<void>,
    validateForm: (values?: Values) => Promise<FormikErrors<Values>>,
    validateField: (name: $Keys<Values>) => Promise<?string>,
    isValid: boolean,
    dirty: boolean,
    unregisterField: (name: string) => void,
    registerField: (name: string, options: {| validate: any |}) => void,
    getFieldProps<Name>(
      name: Name | UseFieldConfig<Name>
    ): [
      FieldInputProps<$ElementType<Values, Name>>,
      FieldMetaProps<$ElementType<Values, Name>>
    ],
    validateOnBlur: boolean,
    validateOnChange: boolean,
    values: Values,
    errors: FormikErrors<Values>,
    touched: FormikTouched<Values>,
    isSubmitting: boolean,
    isValidating: boolean,
    status?: any,
    submitCount: number,
    ...
  };

  declare export var Formik: { <Values>(props: FormikConfig<Values>): React$Node, ... };

  declare export function yupToFormErrors<Values>(
    yupError: YupError
  ): FormikErrors<Values>;

  declare export function validateYupSchema<Values>(
    values: Values,
    schema: Schema,
    sync?: boolean,
    context?: any
  ): Promise<$Shape<Values>>;
}

declare module 'formik/@connect' {
  declare export function connect<Config, Comp: React$ComponentType<Config>>(
    c: Comp
  ): React$ComponentType<$Diff<React$ElementConfig<Comp>, { formik: any, ... }>>;
}

declare module 'formik' {
  declare export * from 'formik/@connect'
  declare export * from 'formik/@ErrorMessage'
  declare export * from 'formik/@Field'
  declare export * from 'formik/@FieldArray'
  declare export * from 'formik/@flow-typed'
  declare export * from 'formik/@flow-typed'
  declare export * from 'formik/@Form'
  declare export * from 'formik/@Formik'
  declare export * from 'formik/@FormikContext'
  declare export * from 'formik/@utils'
  declare export * from 'formik/@withFormik'
}
