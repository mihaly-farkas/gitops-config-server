import {getContainerHealthStatus} from './get-container-health-status';
import {runCommand} from '../bash/run-command';
import {expect} from 'chai';
import {getContainerStatus} from './get-container-status';

const waitIterations = 30;
const waitInterval = 1000;

export const waitForContainerToBeHealthy = (containerName: string) => {
  let iteration = 0;
  let status;

  expect(getContainerStatus(containerName)).to.equal('running');

  while (status !== 'healthy' && iteration < waitIterations) {
    status = getContainerHealthStatus(containerName);
    runCommand(`sleep ${waitInterval / 1000}`);
    iteration++;
  }

  if (status !== 'healthy') {
    throw new Error(
      `The "${containerName}" container did not become healthy within the expected time. Current status: ${status}. ` +
        'Please check the container logs for more details.',
    );
  }
};
