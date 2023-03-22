const getConfig = (configType, configuration) => configuration?.[configType] ?? null;

export { getConfig };
export default getConfig;
