import {runCommand} from '../bash/run-command';

export const getContainerHealthStatus = (containerName: string): string =>
  runCommand(`docker inspect --format='{{.State.Health.Status}}' ${containerName}`);
