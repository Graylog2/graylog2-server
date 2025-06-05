import { singleton } from 'logic/singleton';

export interface PrefixMapper {
  mapForIdAndType(id: string, type: string): string | undefined;
  mapForType(type: string): string | undefined;
}

class PrefixMapperBinder {
  static __registrations: Array<PrefixMapper> = [];

  static register(implementingClass: PrefixMapper) {
    this.__registrations.push(implementingClass);
  }

  static mapToPrefix(type: string, id: string): string | undefined {
    const mapForIdAndType = this.__registrations
      .map((mapper) => mapper.mapForIdAndType(id, type))
      .find((prefix) => prefix !== undefined);

    return (
      mapForIdAndType ||
      this.__registrations.map((mapper) => mapper.mapForType(type)).find((prefix) => prefix !== undefined)
    );
  }
}

const SingletonPrefixMapperBinder = singleton('logic.prefixmapper.binder', () => PrefixMapperBinder);
// eslint-disable-next-line @typescript-eslint/no-redeclare
type SingletonPrefixMapperBinder = InstanceType<typeof PrefixMapperBinder>;

export default SingletonPrefixMapperBinder;
