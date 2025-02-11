export type Processor = {
  name: string,
  class_name: string
};
export type ProcessorConfig = {
  disabled_processors: Array<string>,
  processor_order: Array<Processor>,
};
export type GlobalProcessingConfig = {
  enableFutureTimestampNormalization?: boolean,
  grace_period?: string,
};
export type FormConfig = ProcessorConfig & GlobalProcessingConfig;
