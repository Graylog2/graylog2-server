export type QueryValidationState = {
  status: 'OK' | 'ERROR' | 'WARNING',
  explanations: Array<{
    errorType: string,
    errorMessage: string,
    beginLine: number,
    endLine: number,
    beginColumn: number,
    endColumn: number,
  }> | undefined
}
