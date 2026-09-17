const test = require('node:test');
const assert = require('node:assert/strict');
const {
  resolveCameraFailure,
  computeResumeIntent,
  shouldRestartCamera,
  bindCameraControls
} = require('./smartshot_camera_helpers.js');

function createMockButton() {
  const listeners = {};
  return {
    addEventListener(type, handler) {
      listeners[type] = listeners[type] || [];
      listeners[type].push(handler);
    },
    click() {
      (listeners.click || []).forEach((handler) => handler());
    }
  };
}

test('computeResumeIntent only resumes when SmartShot is active and a stream exists', () => {
  assert.equal(computeResumeIntent(true, true), true);
  assert.equal(computeResumeIntent(true, false), false);
  assert.equal(computeResumeIntent(false, true), false);
});

test('shouldRestartCamera only restarts on visible state with active SmartShot and resume intent', () => {
  assert.equal(shouldRestartCamera('visible', true, true, false), true);
  assert.equal(shouldRestartCamera('visible', false, true, false), false);
  assert.equal(shouldRestartCamera('hidden', true, true, false), false);
  assert.equal(shouldRestartCamera('visible', true, false, false), false);
  assert.equal(shouldRestartCamera('visible', true, true, true), false);
});

test('resolveCameraFailure returns specific guidance for denied, insecure, and unavailable cases', () => {
  assert.deepEqual(resolveCameraFailure({ name: 'NotAllowedError' }, true), {
    title: 'CÂMERA BLOQUEADA',
    detail: 'Permita acesso ou use o upload manual',
    message: 'Permita o acesso à câmera para continuar.'
  });
  assert.deepEqual(resolveCameraFailure({ name: 'NotReadableError' }, false), {
    title: 'CONTEXTO INSEGURO',
    detail: 'Abra a página em HTTPS ou localhost',
    message: 'Abra a página em HTTPS ou localhost para usar a câmera.'
  });
  assert.deepEqual(resolveCameraFailure({ name: 'NotFoundError' }, true), {
    title: 'CÂMERA INDISPONÍVEL',
    detail: 'Nenhum dispositivo compatível foi encontrado',
    message: 'Não encontrei uma câmera disponível neste dispositivo.'
  });
});

test('bindCameraControls wires shutter and flip handlers', () => {
  const shutterA = createMockButton();
  const shutterB = createMockButton();
  const flipA = createMockButton();
  let captures = 0;
  let flips = 0;
  bindCameraControls([shutterA, shutterB], [flipA], () => { captures += 1; }, () => { flips += 1; });
  shutterA.click();
  shutterB.click();
  flipA.click();
  assert.equal(captures, 2);
  assert.equal(flips, 1);
});
