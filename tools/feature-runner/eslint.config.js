const chaiFriendly = require('eslint-plugin-chai-friendly');

let customConfig = [];
let hasIgnoresFile = false;

try {
  require.resolve('./eslint.ignores.js');
  hasIgnoresFile = true;
} catch {
  // eslint.ignores.js doesn't exist
}

if (hasIgnoresFile) {
  const ignores = require('./eslint.ignores.js');
  customConfig = [{ignores}];
}

module.exports = [
  ...customConfig,
  ...require('gts'),
  {
    plugins: {
      'chai-friendly': chaiFriendly,
    },

    rules: {
      'max-len': [
        'error',
        {
          code: 120,
        },
      ],
      '@typescript-eslint/no-unused-expressions': 'off',
      'chai-friendly/no-unused-expressions': 'error',
    },
  },
];
