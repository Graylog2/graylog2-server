// @flow strict

export type VisualizationConfigJson = {
  type: string,
};

export default class VisualizationConfig {
  static fromJSON(type: string, value: any): VisualizationConfig {
    const implementingClass = VisualizationConfig.__registrations[type.toLocaleLowerCase()];

    if (implementingClass) {
      return implementingClass.fromJSON(type, value);
    }

    throw new Error(`Unable to find visualization config of type: ${type} - missing plugin?`);
  }

  // eslint-disable-next-line class-methods-use-this
  toBuilder() {
    throw new Error('Must not be called on abstract class!');
  }

  static __registrations: { [string]: typeof VisualizationConfig } = {};

  static registerSubtype(type: string, implementingClass: typeof VisualizationConfig) {
    this.__registrations[type.toLocaleLowerCase()] = implementingClass;
  }
}
