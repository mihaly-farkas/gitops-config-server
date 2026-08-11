import {stopContainer} from './stop-container';
import {removeContainer} from './remove-container';

export const stopAndRemoveContainer = (containerName: string) => {
  stopContainer(containerName);
  removeContainer(containerName);
};
