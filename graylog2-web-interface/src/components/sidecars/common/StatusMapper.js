const StatusMapper = {
  toString(statusCode) {
    switch (Number(statusCode)) {
      case 0:
        return 'Running';
      case 2:
        return 'Failing';
      default:
        return 'Unknown';
    }
  },
};

export default StatusMapper;