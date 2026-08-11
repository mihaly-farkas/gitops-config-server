import {AfterAll, BeforeAll} from '@cucumber/cucumber';
import {existsSync, lstatSync, readlinkSync, rmSync, symlinkSync, unlinkSync} from 'node:fs';
import {stopAndRemoveContainer} from '../utils/docker/stop-and-remove-container';

const originalFeaturesDir = '../../features';
const localFeaturesDir = './features';

BeforeAll(async () => {
  // If the local runtime environment is running on an operating system that supports symlinks, ...
  if (process.platform !== 'win32') {
    if (existsSync(localFeaturesDir)) {
      const stats = lstatSync(localFeaturesDir);
      if (stats.isSymbolicLink()) {
        if (readlinkSync(localFeaturesDir) !== originalFeaturesDir) {
          unlinkSync(localFeaturesDir);
        }
      } else if (stats.isDirectory()) {
        rmSync(localFeaturesDir, {recursive: true});
      }
    }
    if (!existsSync(localFeaturesDir)) {
      symlinkSync(originalFeaturesDir, localFeaturesDir, 'dir');
    }
  } else {
    console.error(
      'ERROR: Symbolic links are not supported on Windows. Please run this script on a Unix-based operation system.',
    );
  }

  stopAndRemoveContainer('gitops-config-server.minimal');
  stopAndRemoveContainer('gitops-config-server.encrypt-key');
});

AfterAll(async () => {
  stopAndRemoveContainer('gitops-config-server.minimal');
  stopAndRemoveContainer('gitops-config-server.encrypt-key');
});
