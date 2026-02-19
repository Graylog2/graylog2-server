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
import * as React from 'react';

import loadAsync from './loadAsync';

/*
 * Type-level tests for loadAsync.
 *
 * loadAsync returns React.FC<TProps> rather than React.ComponentType<TProps>.
 * This is intentional: ComponentType is a union of FunctionComponent | ComponentClass,
 * and ComponentClass uses Readonly<T> in its constructor, making ComponentType<T>
 * invariant in T. FC uses a call signature which gets bivariant checking, matching
 * the assignability behavior of directly imported components.
 *
 * These tests use @ts-expect-error to verify that type errors occur where expected
 * and that valid assignments compile without error.
 */

// -- Test components ---------------------------------------------------------

const ExactPropsComponent = loadAsync<{ name: string }>(
  () => Promise.resolve({ default: ({ name }: { name: string }) => React.createElement('span', null, name) }),
);

const SubsetPropsComponent = loadAsync<{ name: string }>(
  () => Promise.resolve({ default: ({ name }: { name: string }) => React.createElement('span', null, name) }),
);

const RefPropsComponent = loadAsync<{ dashboard: string } & React.RefAttributes<{ open: () => void }>>(
  () => Promise.resolve({
    default: React.forwardRef<{ open: () => void }, { dashboard: string }>((_props, _ref) => null),
  }),
);

// -- Binding-like target types -----------------------------------------------

type SystemConfigurationComponentProps = {
  config?: unknown;
  updateConfig: (newConfig: unknown) => void;
};

type SystemConfiguration = {
  component: React.ComponentType<SystemConfigurationComponentProps>;
};

type DashboardActionModalProps<T> = React.PropsWithRef<{ dashboard: string }> & {
  ref: React.LegacyRef<T>;
};

type DashboardAction<T> = {
  modal: React.ComponentType<DashboardActionModalProps<T>>;
};

type WidgetExportBinding = {
  exportComponent: React.ComponentType<{ widget: string }>;
};

type SupersetPropsBinding = {
  component: React.ComponentType<{ name: string; extra: number }>;
};

// -- Type-level assertions ---------------------------------------------------

describe('loadAsync type-level tests', () => {
  /*
   * Assigning a loadAsync component to a binding that expects ComponentType
   * with compatible props must compile. This is the primary use case: plugin
   * binding objects use ComponentType<SpecificProps> and loadAsync-wrapped
   * components must be assignable to those slots.
   */
  it('is assignable to ComponentType<T> with exact props', () => {
    const _binding: { component: React.ComponentType<{ name: string }> } = {
      component: ExactPropsComponent,
    };

    expect(_binding).toBeDefined();
  });

  /*
   * A component whose props are a subset of the target type should be
   * assignable due to function bivariance. This matches the behavior of
   * directly imported components.
   */
  it('is assignable to ComponentType with superset props (bivariance)', () => {
    const _binding: SupersetPropsBinding = {
      component: SubsetPropsComponent,
    };

    expect(_binding).toBeDefined();
  });

  /*
   * A component with RefAttributes in its props should be assignable to
   * a binding that expects props with a required ref field (e.g. modal
   * bindings using React.PropsWithRef + ref: LegacyRef<T>).
   */
  it('is assignable to ComponentType with ref-bearing props', () => {
    const _binding: DashboardAction<{ open: () => void }> = {
      modal: RefPropsComponent,
    };

    expect(_binding).toBeDefined();
  });

  /*
   * A loadAsync component with unknown props (no explicit type parameter)
   * should still be assignable to ComponentType<SpecificProps> bindings.
   * This covers cases where module default exports lack explicit prop types.
   */
  it('is assignable to ComponentType<SpecificProps> when inferred as unknown', () => {
    const UnknownPropsComponent = loadAsync<unknown>(
      () => Promise.resolve({ default: () => null }),
    );

    const _binding: SystemConfiguration = {
      component: UnknownPropsComponent,
    };

    expect(_binding).toBeDefined();
  });

  /*
   * Negative test: a component with incompatible props (completely unrelated
   * types) should NOT be assignable. This verifies that the bivariance does
   * not eliminate all type safety.
   */
  it('rejects completely incompatible prop types', () => {
    const NumberComponent = loadAsync<{ count: number }>(
      () => Promise.resolve({ default: ({ count }: { count: number }) => React.createElement('span', null, count) }),
    );

    const _binding: WidgetExportBinding = {
      // @ts-expect-error - { count: number } is not assignable to { widget: string }
      exportComponent: NumberComponent,
    };

    expect(_binding).toBeDefined();
  });
});
