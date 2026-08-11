import {getContainerProperty} from './get-container-property';

export const getContainerStatus = (containerName: string): string =>
  getContainerProperty(containerName, '.State.Status');
