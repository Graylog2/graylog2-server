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

import { INPUT_WIZARD_STEPS } from 'components/inputs/InputSetupWizard/types';
import type { StepData, StepsData } from 'components/inputs/InputSetupWizard/types';

import {
  getStepData,
  getNextStep,
  checkHasNextStep,
  checkHasPreviousStep,
  checkIsNextStepDisabled,
  addStepAfter,
  updateStepData,
  enableNextStep,
} from './stepHelper';

const stepsData = {
  [INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS]: {
    foo: 'foo1',
    bar: 'bar1',
  },
  [INPUT_WIZARD_STEPS.SELECT_CATEGORY]: {
    aloho: 'aloho1',
    mora: 'mora1',
  },
};

const orderedSteps = [INPUT_WIZARD_STEPS.SELECT_CATEGORY, INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS];

describe('stepHelper', () => {
  describe('getStepData', () => {
    it('returns data for specific step', () => {
      expect(getStepData(stepsData as StepsData, INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS)).toEqual(
        stepsData[INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS],
      );
    });

    it('returns undefined if no step data exists', () => {
      expect(getStepData(stepsData as StepsData, INPUT_WIZARD_STEPS.SETUP_ROUTING)).toEqual(
        undefined,
      );
    });
  });

  describe('getNextStep', () => {
    it('returns the next step', () => {
      expect(getNextStep(orderedSteps, INPUT_WIZARD_STEPS.SELECT_CATEGORY)).toEqual(
        INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS,
      );
    });

    it('returns undefined if there is no next step', () => {
      expect(getNextStep(orderedSteps, INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS)).toEqual(
        undefined,
      );
    });

    it('returns undefined if active step is not in ordered steps', () => {
      expect(getNextStep(orderedSteps, INPUT_WIZARD_STEPS.SETUP_ROUTING)).toEqual(
        undefined,
      );
    });
  });

  describe('checkHasNextStep', () => {
    it('returns true when there is a next step', () => {
      expect(checkHasNextStep(orderedSteps, INPUT_WIZARD_STEPS.SELECT_CATEGORY)).toBe(true);
    });

    it('returns false when there is no next step', () => {
      expect(checkHasNextStep(orderedSteps, INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS)).toBe(false);
    });

    it('returns false when the active step is not part of orderedSteps', () => {
      expect(checkHasNextStep(orderedSteps, INPUT_WIZARD_STEPS.SETUP_ROUTING)).toBe(false);
    });
  });

  describe('checkHasPreviousStep', () => {
    it('returns true when there is a previous step', () => {
      expect(checkHasPreviousStep(orderedSteps, INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS)).toBe(true);
    });

    it('returns false when there is no previous step', () => {
      expect(checkHasPreviousStep(orderedSteps, INPUT_WIZARD_STEPS.SELECT_CATEGORY)).toBe(false);
    });

    it('returns false when the active step is not part of orderedSteps', () => {
      expect(checkHasPreviousStep(orderedSteps, INPUT_WIZARD_STEPS.SETUP_ROUTING)).toBe(false);
    });
  });

  describe('checkIsNextStepDisabled', () => {
    it('returns true when the next step is disabled', () => {
      const testStepsData = {
        [INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS]: {
          enabled: false,
        },
        [INPUT_WIZARD_STEPS.SELECT_CATEGORY]: {
          enabled: true,
        },
      };

      expect(checkIsNextStepDisabled(orderedSteps, INPUT_WIZARD_STEPS.SELECT_CATEGORY, testStepsData as StepsData)).toBe(true);
    });

    it('returns false when the next step is not disabled', () => {
      const testStepsData = {
        [INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS]: {
          enabled: true,
        },
        [INPUT_WIZARD_STEPS.SELECT_CATEGORY]: {
          enabled: true,
        },
      };

      expect(checkIsNextStepDisabled(orderedSteps, INPUT_WIZARD_STEPS.SELECT_CATEGORY, testStepsData as StepsData)).toBe(false);
    });

    it('returns true when there is no data for the next step', () => {
      const testStepsData = {
        [INPUT_WIZARD_STEPS.SELECT_CATEGORY]: {
          enabled: true,
        },
      };

      expect(checkIsNextStepDisabled(orderedSteps, INPUT_WIZARD_STEPS.SELECT_CATEGORY, testStepsData as StepsData)).toBe(true);
    });

    it('returns true there is no next step', () => {
      const testStepsData = {
        [INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS]: {
          enabled: true,
        },
        [INPUT_WIZARD_STEPS.SELECT_CATEGORY]: {
          enabled: true,
        },
      };

      expect(checkIsNextStepDisabled(orderedSteps, INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS, testStepsData as StepsData)).toBe(true);
    });
  });

  describe('enableNextStep', () => {
    it('returns updated steps data with next step enabled', () => {
      const testStepsData = {
        [INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS]: {
          enabled: false,
        },
        [INPUT_WIZARD_STEPS.SELECT_CATEGORY]: {
          enabled: false,
        },
      };

      expect(enableNextStep(orderedSteps, INPUT_WIZARD_STEPS.SELECT_CATEGORY, testStepsData as StepsData)).toEqual({
        [INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS]: {
          enabled: true,
        },
        [INPUT_WIZARD_STEPS.SELECT_CATEGORY]: {
          enabled: false,
        },
      });
    });

    it('returns updated steps data with next step enabled when there is no data for the step yet', () => {
      const testStepsData = {
        [INPUT_WIZARD_STEPS.SELECT_CATEGORY]: {
          enabled: false,
        },
      };

      expect(enableNextStep(orderedSteps, INPUT_WIZARD_STEPS.SELECT_CATEGORY, testStepsData as StepsData)).toEqual({
        [INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS]: {
          enabled: true,
        },
        [INPUT_WIZARD_STEPS.SELECT_CATEGORY]: {
          enabled: false,
        },
      });
    });

    it('returns the original steps data when there is no next step', () => {
      const testStepsData = {
        [INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS]: {
          enabled: false,
        },
        [INPUT_WIZARD_STEPS.SELECT_CATEGORY]: {
          enabled: false,
        },
      };

      expect(enableNextStep(orderedSteps, INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS, testStepsData as StepsData)).toEqual(testStepsData);
    });

    it('returns the original steps data when the active step is not in orderedSteps', () => {
      const testStepsData = {
        [INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS]: {
          enabled: false,
        },
        [INPUT_WIZARD_STEPS.SELECT_CATEGORY]: {
          enabled: false,
        },
      };

      expect(enableNextStep(orderedSteps, INPUT_WIZARD_STEPS.SETUP_ROUTING, testStepsData as StepsData)).toEqual(testStepsData);
    });
  });

  describe('updateStepData', () => {
    it('returns updated steps data with new attribute', () => {
      const testStepsData = {
        [INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS]: {
          enabled: false,
        },
        [INPUT_WIZARD_STEPS.SELECT_CATEGORY]: {
          enabled: false,
        },
      };

      expect(updateStepData(testStepsData as StepsData, INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS, { foo: 'bar' } as StepData)).toEqual({
        [INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS]: {
          enabled: false,
          foo: 'bar',
        },
        [INPUT_WIZARD_STEPS.SELECT_CATEGORY]: {
          enabled: false,
        },
      });
    });

    it('returns updated steps data with updated existing attribute', () => {
      const testStepsData = {
        [INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS]: {
          enabled: false,
          foo: 'foo',
        },
        [INPUT_WIZARD_STEPS.SELECT_CATEGORY]: {
          enabled: true,
          foo: 'foo',
        },
      };

      expect(updateStepData(testStepsData as StepsData, INPUT_WIZARD_STEPS.SELECT_CATEGORY, { foo: 'bar' } as StepData)).toEqual({
        [INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS]: {
          enabled: false,
          foo: 'foo',
        },
        [INPUT_WIZARD_STEPS.SELECT_CATEGORY]: {
          enabled: true,
          foo: 'bar',
        },
      });
    });

    it('returns the original steps data when no data was given', () => {
      const testStepsData = {
        [INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS]: {
          enabled: false,
          foo: 'foo',
        },
        [INPUT_WIZARD_STEPS.SELECT_CATEGORY]: {
          enabled: true,
          foo: 'foo',
        },
      };

      expect(updateStepData(testStepsData as StepsData, INPUT_WIZARD_STEPS.SELECT_CATEGORY, {} as StepData)).toEqual(testStepsData);
    });

    it('returns updated steps data when no step data existed', () => {
      const testStepsData = {
        [INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS]: {
          enabled: false,
          foo: 'foo',
        },
      };

      expect(updateStepData(testStepsData as StepsData, INPUT_WIZARD_STEPS.SELECT_CATEGORY, { foo: 'bar' } as StepData)).toEqual({
        [INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS]: {
          enabled: false,
          foo: 'foo',
        },
        [INPUT_WIZARD_STEPS.SELECT_CATEGORY]: {
          foo: 'bar',
        },
      });
    });

    it('returns new steps data when no steps data existed', () => {
      expect(updateStepData(undefined, INPUT_WIZARD_STEPS.SELECT_CATEGORY, { foo: 'bar' } as StepData)).toEqual({
        [INPUT_WIZARD_STEPS.SELECT_CATEGORY]: {
          foo: 'bar',
        },
      });
    });

    it('returns empty object when no step name is given', () => {
      expect(updateStepData(undefined, undefined, { foo: 'bar' } as StepData)).toEqual({});
    });
  });

  describe('addStepAfter', () => {
    it('returns ordered steps with added step in the middle', () => {
      const testOrderedSteps = [INPUT_WIZARD_STEPS.SELECT_CATEGORY, INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS];

      expect(addStepAfter(testOrderedSteps, INPUT_WIZARD_STEPS.SETUP_ROUTING, INPUT_WIZARD_STEPS.SELECT_CATEGORY)).toEqual(
        [INPUT_WIZARD_STEPS.SELECT_CATEGORY, INPUT_WIZARD_STEPS.SETUP_ROUTING, INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS],
      );
    });

    it('returns ordered steps with added step at the end', () => {
      const testOrderedSteps = [INPUT_WIZARD_STEPS.SELECT_CATEGORY, INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS];

      expect(addStepAfter(testOrderedSteps, INPUT_WIZARD_STEPS.SETUP_ROUTING, INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS)).toEqual(
        [INPUT_WIZARD_STEPS.SELECT_CATEGORY, INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS, INPUT_WIZARD_STEPS.SETUP_ROUTING],
      );
    });

    it('returns ordered steps with added step at the end when no step to set after given', () => {
      const testOrderedSteps = [INPUT_WIZARD_STEPS.SELECT_CATEGORY, INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS];

      expect(addStepAfter(testOrderedSteps, INPUT_WIZARD_STEPS.SETUP_ROUTING)).toEqual(
        [INPUT_WIZARD_STEPS.SELECT_CATEGORY, INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS, INPUT_WIZARD_STEPS.SETUP_ROUTING],
      );
    });

    it('returns original ordered steps when step to set after is not in the array', () => {
      const testOrderedSteps = [INPUT_WIZARD_STEPS.SELECT_CATEGORY, INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS];

      expect(addStepAfter(testOrderedSteps, INPUT_WIZARD_STEPS.SETUP_ROUTING, INPUT_WIZARD_STEPS.START_INPUT)).toEqual(
        testOrderedSteps,
      );
    });
  });
});
