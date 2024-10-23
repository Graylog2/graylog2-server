export const INPUT_WIZARD_STEPS = {
  SELECT_CATEGORY: 'SELECT_CATEGORY',
  PREFLIGHT: 'PREFLIGHT',
  ACTIVATE_ILLUMINATE: 'ACTIVATE_ILLUMINATE',
  TEST_INPUT: 'TEST_INPUT',
  SETUP_ROUTING: 'SETUP_ROUTING',
  CREATE_STREAM: 'CREATE_STREAM',
  COMPLETE: 'COMPLETE',
} as const;

export const INPUT_WIZARD_CATEGORIES = {
  GENERIC: 'GENERIC',
} as const;

export const INPUT_WIZARD_SUBCATEGORIES = {
  GENERIC: 'GENERIC',
} as const;

export type InputSetupWizardStep = typeof INPUT_WIZARD_STEPS[keyof typeof INPUT_WIZARD_STEPS]
export type InputSetupWizardCategory = typeof INPUT_WIZARD_CATEGORIES[keyof typeof INPUT_WIZARD_CATEGORIES]
export type InputSetupWizardSubcategory = typeof INPUT_WIZARD_SUBCATEGORIES[keyof typeof INPUT_WIZARD_SUBCATEGORIES]

export type StepsData = {
  [key in InputSetupWizardStep]?: any
}

export type WizardData = {
  inputId?: string,
  category?: InputSetupWizardCategory
  subcategory?: InputSetupWizardSubcategory
}
