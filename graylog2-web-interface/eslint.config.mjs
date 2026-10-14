import { defineConfig } from 'eslint/config';
import graylog from 'eslint-config-graylog';

// Parts of the UI which are completely translated. Extend the list when translating more of them, so they stay that way.
const TRANSLATED_FILES = [
  'src/components/bootstrap/BootstrapModalConfirm.tsx',
  'src/components/bootstrap/BootstrapModalForm.tsx',
  'src/components/bootstrap/menuitem/DeleteMenuItem.tsx',
  'src/components/common/BetaBadge.tsx',
  'src/components/common/ClipboardContainer.tsx',
  'src/components/common/ConfirmDialog.tsx',
  'src/components/common/ConfirmLeaveDialog.tsx',
  'src/components/common/ControlledTableList.tsx',
  'src/components/common/CreateModal.tsx',
  'src/components/common/CreatePage.tsx',
  'src/components/common/DataTable/**',
  'src/components/common/DefaultLabel.tsx',
  'src/components/common/EmptyEntity.tsx',
  'src/components/common/EnterprisePluginNotFound.tsx',
  'src/components/common/EntityDataTable/**',
  'src/components/common/EntityList.tsx',
  'src/components/common/ErrorAlert.tsx',
  'src/components/common/FetchErrorAlert.tsx',
  'src/components/common/FormSubmit.tsx',
  'src/components/common/InputOptionalInfo.tsx',
  'src/components/common/KeyValueTable.tsx',
  'src/components/common/LoadingIndicator.tsx',
  'src/components/common/MarkdownEditor/**',
  'src/components/common/ModalSubmit.tsx',
  'src/components/common/MoreActions.tsx',
  'src/components/common/NoEntitiesExist.tsx',
  'src/components/common/NoSearchResult.tsx',
  'src/components/common/PageErrorOverview.tsx',
  'src/components/common/PageSizeSelect.tsx',
  'src/components/common/PaginatedEntityTable/**',
  'src/components/common/PaginatedItemOverview/**',
  'src/components/common/Pagination.tsx',
  'src/components/common/PreviewBadge.tsx',
  'src/components/common/RestrictedAccessTooltip.tsx',
  'src/components/common/SearchForm.tsx',
  'src/components/common/Select/**',
  'src/components/common/SelectPopover.tsx',
  'src/components/common/SortIcon.tsx',
  'src/components/common/SourceCodeEditor.tsx',
  'src/components/common/Spinner.tsx',
  'src/components/common/TypeAheadDataFilter.tsx',
  'src/components/common/Wizard.tsx',
];

export default defineConfig([
  {
    extends: [graylog],
  },
  {
    files: TRANSLATED_FILES,
    ignores: ['**/*.test.*', '**/*.stories.*', '**/__tests__/**'],
    rules: {
      'graylog/no-untranslated-text': 'error',
    },
  },
]);
