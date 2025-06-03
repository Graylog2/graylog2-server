import { singleton } from 'logic/singleton';

export interface PermissionChecker {
  checkWithID(type: string, id: string): string | undefined;
  check(type: string): string | undefined;
}

class PermissionsBinder {
  static __registrations: Array<PermissionChecker> = [];

  static register(implementingClass: PermissionChecker) {
    this.__registrations.push(implementingClass);
  }

  static check(type: string, id: string): string | undefined {
    // eslint-disable-next-line no-restricted-syntax
    for (const checker of this.__registrations) {
      const result = checker.checkWithID(type, id);
      if (result) {
        return result;
      }
    }
    // eslint-disable-next-line no-restricted-syntax
    for (const checker of this.__registrations) {
      const result = checker.check(type);
      if (result) {
        return result;
      }
    }

    return undefined;
  }
}

const SingletonPermissionsBinder = singleton('logic.permissions.binder', () => PermissionsBinder);
// eslint-disable-next-line @typescript-eslint/no-redeclare
type SingletonPermissionsBinder = InstanceType<typeof PermissionsBinder>;

export default SingletonPermissionsBinder;
