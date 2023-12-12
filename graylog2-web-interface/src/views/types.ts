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
import type React from 'react';
import type * as Immutable from 'immutable';
import type { FormikErrors } from 'formik';
import type { Reducer, AnyAction } from '@reduxjs/toolkit';

import type Widget from 'views/logic/widgets/Widget';
import type { ActionDefinition } from 'views/components/actions/ActionHandler';
import type { VisualizationComponent } from 'views/components/aggregationbuilder/AggregationBuilder';
import type { WidgetActionType } from 'views/components/widgets/Types';
import type { Creator } from 'views/components/sidebar/create/AddWidgetButton';
import type { ViewHook } from 'views/logic/hooks/ViewHook';
import type WidgetConfig from 'views/logic/widgets/WidgetConfig';
import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import type { Completer } from 'views/components/searchbar/SearchBarAutocompletions';
import type { Result } from 'views/components/widgets/Widget';
import type { OverrideProps } from 'views/components/WidgetOverrideElements';
import type {
  VisualizationConfigDefinition,
  VisualizationConfigFormValues,
  VisualizationFormValues,
  WidgetConfigFormValues,
} from 'views/components/aggregationwizard';
import type VisualizationConfig from 'views/logic/aggregationbuilder/visualizations/VisualizationConfig';
import type { TimeRange, NoTimeRangeOverride, AbsoluteTimeRange, QueryId } from 'views/logic/queries/Query';
import type View from 'views/logic/views/View';
import type User from 'logic/users/User';
import type { Message } from 'views/components/messagelist/Types';
import type { ValuePath } from 'views/logic/valueactions/ValueActionHandler';
import type WidgetPosition from 'views/logic/widgets/WidgetPosition';
import type MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import type { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';
import type Query from 'views/logic/queries/Query';
import type { CustomCommand, CustomCommandContext } from 'views/components/searchbar/queryinput/types';
import type SearchExecutionState from 'views/logic/search/SearchExecutionState';
import type { ParameterBindings } from 'views/logic/search/SearchExecutionState';
import type SearchMetadata from 'views/logic/search/SearchMetadata';
import type { AppDispatch } from 'stores/useAppDispatch';
import type SearchResult from 'views/logic/SearchResult';
import type { WidgetMapping } from 'views/logic/views/types';
import type { Event } from 'components/events/events/types';
import type Parameter from 'views/logic/parameters/Parameter';
import type { UndoRedoState } from 'views/logic/slices/undoRedoSlice';
import type { SearchExecutors } from 'views/logic/slices/searchExecutionSlice';

export type ArrayElement<ArrayType extends readonly unknown[]> =
  ArrayType extends readonly (infer ElementType)[] ? ElementType : never;

export type BackendWidgetPosition = {
  id: string,
  col: number,
  row: number,
  height: number,
  width: number,
};

export type WidgetPositions = { [widgetId: string]: WidgetPosition };

export interface EditWidgetComponentProps<Config extends WidgetConfig = WidgetConfig> {
  children: React.ReactNode,
  config: Config,
  editing: boolean;
  id: string;
  type: string;
  fields: Immutable.List<FieldTypeMapping>,
  onChange: (newConfig: Config) => void,
  onSubmit: () => void,
  onCancel: () => void,
}

export interface WidgetResults {
 [key: string]: Result,
}

export interface WidgetComponentProps<Config extends WidgetConfig = WidgetConfig, Results = WidgetResults> {
  config: Config;
  data: Results;
  editing: boolean;
  fields: Immutable.List<FieldTypeMapping>;
  filter: string;
  queryId: string;
  onConfigChange: (newConfig: Config) => Promise<void>;
  setLoadingState: (loading: boolean) => void;
  title: string;
  toggleEdit: () => void;
  type: string;
  id: string;
}

export interface WidgetExport {
  type: string;
  displayName?: string;
  defaultHeight?: number;
  defaultWidth?: number;
  visualizationComponent: React.ComponentType<WidgetComponentProps<any, any>>;
  editComponent: React.ComponentType<EditWidgetComponentProps<any>>;
  hasEditSubmitButton?: boolean,
  needsControlledHeight: (widget: { config: Widget['config'] }) => boolean;
  searchResultTransformer?: (data: Array<unknown>) => unknown;
  searchTypes: (widget: Widget) => Array<any>;
  titleGenerator?: (widget: { config: Widget['config'] }) => string;
  reportStyle?: () => { width: React.CSSProperties['width'] };
  exportComponent?: React.ComponentType<{ widget: Widget }>;
}

export interface VisualizationConfigProps {
  config: WidgetConfig;
  onChange: (newConfig: WidgetConfig) => void;
}

type BaseField = {
  title: string,
  name: string,
  helpComponent?: React.ComponentType,
  description?: string,
  isShown?: (formValues: VisualizationConfigFormValues) => boolean,
};

type BaseRequiredField = BaseField & {
  required: boolean,
};

type SelectField = BaseRequiredField & {
  type: 'select',
  options: ReadonlyArray<string | [string, any]>,
};

type MultiSelectField = BaseRequiredField & {
  type: 'multi-select',
  options: ((props: any) => ReadonlyArray<string | [string, any]>) | ReadonlyArray<string | [string, any]>,
};

type BooleanField = BaseField & {
  type: 'boolean',
};

export type NumericField = BaseRequiredField & {
  type: 'numeric',
  step?: string,
};

export type ConfigurationField = SelectField | BooleanField | NumericField | MultiSelectField;

export interface VisualizationCapabilities {
  'event-annotations': undefined,
}

export type VisualizationCapability = keyof VisualizationCapabilities;

export interface VisualizationType<Type extends string, ConfigType extends VisualizationConfig = VisualizationConfig, ConfigFormValuesType extends VisualizationConfigFormValues = VisualizationConfigFormValues> {
  type: string;
  displayName: string;
  component: VisualizationComponent<Type>;
  config?: VisualizationConfigDefinition<ConfigType, ConfigFormValuesType>;
  capabilities?: Array<VisualizationCapability>;
  validate?: (formValues: WidgetConfigFormValues) => FormikErrors<VisualizationFormValues>;
}

interface ResultHandler<T, R> {
  convert: (result: T) => R;
}

interface SearchType<T, R> {
  type: string;
  handler: ResultHandler<T, R>;
  defaults: {};
}

interface ExportFormat {
  type: string;
  displayName: () => string;
  disabled?: () => boolean;
  mimeType: string;
  fileExtension: string;
}

export interface SystemConfigurationComponentProps {
  config: any,
  updateConfig: (newConfig: any) => any,
}

export interface SystemConfiguration {
  configType: string;
  component: React.ComponentType<SystemConfigurationComponentProps>;
}

export type SearchTypeResult = {
  type: string,
  effective_timerange: TimeRange,
};

export type MessageResult = {
  type: 'messages',
  total: number,
  effectiveTimerange: AbsoluteTimeRange,
};

export interface SearchTypeResultTypes {
  generic: SearchTypeResult,
  messages: MessageResult,
}

export interface ActionContexts {
  view: View,
  analysisDisabledFields: Array<string>,
  currentUser: User,
  widget: Widget,
  message: Message,
  valuePath: ValuePath,
  isLocalNode: boolean,
  parameters?: Immutable.Set<Parameter>,
  parameterBindings?: ParameterBindings,
}

export type SearchTypeResults = { [id: string]: SearchTypeResultTypes[keyof SearchTypeResultTypes] };

export type MessagePreviewOption = {
  title: string,
  isChecked: (config: MessagesWidgetConfig) => boolean,
  isDisabled: (config: MessagesWidgetConfig) => boolean,
  help?: string,
  onChange: (config: MessagesWidgetConfig, onConfigChange: (config: MessagesWidgetConfig) => void) => void
  sort: number,
}

type ExternalActionsHookData = {
      error: Error | null;
      externalValueActions: Array<ActionDefinition> | null;
      isLoading: boolean;
      isError: boolean
}

type MessageAugmentation = {
  id: string,
  component: React.ComponentType<{ message: Message }>,
}

type MessageDetailContextProviderProps = {
  message: Message,
}

type DashboardActionComponentProps = {
  dashboard: View,
  modalRef: () => unknown,
}

type DashboardActionModalProps = {
  dashboard: View,
  ref: React.Ref<unknown>,
}

type AssetInformationComponentProps = {
  identifiers: unknown,
  addToQuery: (id: string) => void;
}

type DashboardAction = {
  key: string,
  component: React.ComponentType<DashboardActionComponentProps>,
  modal?: React.ComponentType<DashboardActionModalProps>,
}

type AssetInformation = {
  component: React.ComponentType<AssetInformationComponentProps>,
}

type EventActionComponentProps = {
  event: Event,
}

type MessageActionComponentProps = {
  index: string,
  id: string,
}

type SearchActionComponentProps = {
  loaded: boolean,
  view: View,
}

export type CopyParamsToView = (sourceView: View, targetView: View) => View;

type RemovingWidgetHook = (widgetId: string, dashboardId: string) => boolean;

interface MessageRowOverrideProps {
  messageFields: Message['fields'],
  config: MessagesWidgetConfig,
  renderMessageRow: () => React.ReactNode,
}

export interface CombinedSearchBarFormValues {
  timerange?: TimeRange | NoTimeRangeOverride,
  streams?: Array<string>,
  queryString?: string,
}

export interface HandlerContext {
  view: View;
  executionState: SearchExecutionState;
}

export interface SearchBarControl {
  component: React.ComponentType;
  id: string;
  onSearchSubmit?: <T extends Query | undefined>(values: CombinedSearchBarFormValues, dispatch: AppDispatch, currentQuery?: T) => Promise<T>,
  onDashboardWidgetSubmit: (values: CombinedSearchBarFormValues, dispatch: AppDispatch, currentWidget: Widget) => Promise<Widget | void>,
  onValidate?: (values: CombinedSearchBarFormValues, context: HandlerContext) => FormikErrors<{}>,
  placement: 'left' | 'right';
  useInitialSearchValues?: (currentQuery?: Query) => ({ [key: string]: any }),
  useInitialDashboardWidgetValues?: (currentWidget: Widget) => ({ [key: string]: any }),
  validationPayload?: (values: CombinedSearchBarFormValues, context: HandlerContext) => ({ [key: string]: any }),
}

export type SearchFilter = {
  type: 'referenced' | 'inlineQueryString',
  id?: string,
  title?: string,
  description?: string
  queryString: string
  negation?: boolean,
  disabled?: boolean,
}

export type FiltersType = Immutable.List<SearchFilter>

export type SaveViewControls = {
  component: React.ComponentType<{ disabledViewCreation?: boolean }>,
  id: string,
  onSearchDuplication?: (view: View, userPermissions: Immutable.List<string>) => Promise<View>,
  onDashboardDuplication?: (view: View, userPermissions: Immutable.List<string>) => Promise<View>,
}

export type CustomCommandContextProvider<T extends keyof CustomCommandContext> = {
  key: T,
  provider: () => CustomCommandContext[T],
}

export interface ViewState {
  activeQuery: QueryId;
  view: View;
  isDirty: boolean;
  isNew: boolean;
}

export type SearchExecutionResult = {
  result: SearchResult,
  widgetMapping: WidgetMapping,
};

export interface SearchExecution {
  executionState: SearchExecutionState;
  result: SearchExecutionResult;
  isLoading: boolean;
  widgetsToSearch: Array<string>,
}

export interface SearchMetadataState {
  isLoading: boolean;
  metadata: SearchMetadata;
}

export interface RootState {
  view: ViewState;
  searchExecution: SearchExecution;
  searchMetadata: SearchMetadataState;
  undoRedo: UndoRedoState
}

export interface ExtraArguments {
  searchExecutors: SearchExecutors;
}

export type GetState = () => RootState;

export type ViewsReducer = {
  key: keyof RootState,
  reducer: Reducer<RootState[keyof RootState], AnyAction>,
}

export type Widgets = Immutable.OrderedMap<string, Widget>;

export interface WidgetCreatorArgs {
  view: View;
}
export interface WidgetCreator {
  title: string;
  func: (args: WidgetCreatorArgs) => Widget;
  icon: React.ComponentType<{}>,
}

declare module 'graylog-web-plugin/plugin' {
  export interface PluginExports {
    creators?: Array<Creator>;
    enterpriseWidgets?: Array<WidgetExport>;
    useExternalActions?: Array<() => ExternalActionsHookData>,
    fieldActions?: Array<ActionDefinition>;
    messageAugmentations?: Array<MessageAugmentation>;
    searchTypes?: Array<SearchType<any, any>>;
    systemConfigurations?: Array<SystemConfiguration>;
    valueActions?: Array<ActionDefinition>;
    'views.completers'?: Array<Completer>;
    'views.components.assetInformationActions'?: Array<AssetInformation>;
    'views.components.dashboardActions'?: Array<DashboardAction>;
    'views.components.eventActions'?: Array<{
      component: React.ComponentType<EventActionComponentProps>,
      key: string,
    }>;
    'views.components.widgets.messageTable.previewOptions'?: Array<MessagePreviewOption>;
    'views.components.widgets.messageTable.messageRowOverride'?: Array<React.ComponentType<MessageRowOverrideProps>>;
    'views.components.widgets.messageDetails.contextProviders'?: Array<React.ComponentType<React.PropsWithChildren<MessageDetailContextProviderProps>>>;
    'views.components.widgets.messageTable.contextProviders'?: Array<React.ComponentType<React.PropsWithChildren<{}>>>;
    'views.components.widgets.messageTable.messageActions'?: Array<{
      component: React.ComponentType<MessageActionComponentProps>,
      key: string,
    }>;
    'views.components.searchActions'?: Array<{
      component: React.ComponentType<SearchActionComponentProps>,
      key: string,
    }>;
    'views.components.searchBar'?: Array<() => SearchBarControl | null>;
    'views.components.saveViewForm'?: Array<() => SaveViewControls | null>;
    'views.elements.header'?: Array<React.ComponentType>;
    'views.elements.queryBar'?: Array<React.ComponentType>;
    'views.elements.validationErrorExplanation'?: Array<React.ComponentType<{ validationState: QueryValidationState }>>;
    'views.export.formats'?: Array<ExportFormat>;
    'views.hooks.confirmDeletingDashboard'?: Array<(view: View) => Promise<boolean | null>>,
    'views.hooks.confirmDeletingDashboardPage'?: Array<(dashboardId: string, queryId: string, widgetIds: { [queryId: string]: Array<string> }) => Promise<boolean | null>>,
    'views.hooks.confirmDeletingWidget'?: Array<(widget: Widget, view: View, title: string) => Promise<boolean | null>>,
    'views.hooks.executingView'?: Array<ViewHook>;
    'views.hooks.loadingView'?: Array<ViewHook>;
    'views.hooks.copyWidgetToDashboard'?: Array<CopyParamsToView>;
    'views.hooks.copyPageToDashboard'?: Array<CopyParamsToView>;
    'views.hooks.removingWidget'?: Array<RemovingWidgetHook>;
    'views.overrides.widgetEdit'?: Array<React.ComponentType<OverrideProps>>;
    'views.widgets.actions'?: Array<WidgetActionType>;
    'views.reducers'?: Array<ViewsReducer>;
    'views.requires.provided'?: Array<string>;
    'views.queryInput.commands'?: Array<CustomCommand>;
    'views.queryInput.commandContextProviders'?: Array<CustomCommandContextProvider<any>>,
    visualizationTypes?: Array<VisualizationType<any>>;
    widgetCreators?: Array<WidgetCreator>;
  }
}
