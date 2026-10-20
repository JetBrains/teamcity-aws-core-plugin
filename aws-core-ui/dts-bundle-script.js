const dts = require('dts-bundle');

dts.bundle({
  name: 'awsConnectionUiComponents',
  main: 'dist/types/components.d.ts',
  out: '../components.d.ts',
  removeSource: true, // Removes the source .d.ts files (optional)
  outputAsModuleFolder: true, // to keep the external module names unchanged (optional)
});
