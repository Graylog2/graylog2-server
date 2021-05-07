const parsedTimeoutMultiplier = Number.parseFloat(process.env.TIMEOUT_MULTIPLIER);

export const timeoutMultiplier = () => (Number.isFinite(parsedTimeoutMultiplier) ? parsedTimeoutMultiplier : 1.0);

export const applyTimeoutMultiplier = (x) => x * timeoutMultiplier();
