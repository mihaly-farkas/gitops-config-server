import {runCommand} from '../bash/run-command';

export const getContainerProperty = (containerName: string, format: string): string =>
  runCommand(`docker inspect --format='{{${format}}}' ${containerName}`);
