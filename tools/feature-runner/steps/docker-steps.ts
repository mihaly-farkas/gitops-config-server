import {Given, Then, When} from '@cucumber/cucumber';
import {expect} from 'chai';
import {stopAndRemoveContainer} from '../utils/docker/stop-and-remove-container';
import {waitForContainerToBeHealthy} from '../utils/docker/wait-for-container-to-be-healthy';
import {verifyDocker} from '../utils/docker/verify-docker';
import {getContainerStatus} from '../utils/docker/get-container-status';
import {getContainerProperty} from '../utils/docker/get-container-property';
import {runCommand} from '../utils/bash/run-command';
import {isContainerExists} from '../utils/docker/is-container-exists';

Given('Docker is running on my machine', verifyDocker);

When('the {string} container does not exist', stopAndRemoveContainer);

When('I remove the {string} container', stopAndRemoveContainer);

When('if the {string} container is not running, I run the following command:', (containerName, bashCommand) => {
  const containerExists = isContainerExists(containerName);
  if (!containerExists) {
    runCommand(bashCommand);
  }

  const status = getContainerStatus(containerName);

  expect(status).to.equal(
    'running',
    `Expected the "${containerName}" container to be running, but its status is "${status}".` +
      ' Please check the container logs for more details.',
  );
});

When('I wait until the {string} container is healthy', waitForContainerToBeHealthy);

When('I stop and remove the {string} container', containerName => {
  stopAndRemoveContainer(containerName);
});

Then('the {string} container should be running', containerName => {
  expect(getContainerStatus(containerName)).to.equal('running');
});

Then('the {string} container should have a healthcheck configured', containerName => {
  expect(getContainerStatus(containerName)).to.equal('running');
  expect(getContainerProperty(containerName, '.Config.Healthcheck.Test')).to.be.not.empty;
});

Then('the {string} container should be healthy', waitForContainerToBeHealthy);
