declare global {
  namespace NodeJS {
    interface Global {
      pluginNames: Array<string>
    }
  }
}

export {};
