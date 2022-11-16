export type EventDefinition = {
  id: string,
  config: {
    type: string,
  },
  title: string,
  description: string,
  priority: number,
  _scope: string,
};
