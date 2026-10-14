import { defineConfig } from 'eslint/config';
import graylog from 'eslint-config-graylog';

// Parts of the UI which are completely translated. Extend the list when translating more of them, so they stay that way.
const TRANSLATED_FILES = [
  'src/components/bootstrap/BootstrapModalConfirm.tsx',
  'src/components/bootstrap/BootstrapModalForm.tsx',
  'src/components/bootstrap/menuitem/DeleteMenuItem.tsx',
  'src/components/common/ConfirmDialog.tsx',
  'src/components/common/ConfirmLeaveDialog.tsx',
  'src/components/common/FormSubmit.tsx',
  'src/components/common/ModalSubmit.tsx',
];

export default defineConfig([
  {
    extends: [graylog],
  },
  {
    files: TRANSLATED_FILES,
    rules: {
      'graylog/no-untranslated-text': 'error',
    },
  },
]);
