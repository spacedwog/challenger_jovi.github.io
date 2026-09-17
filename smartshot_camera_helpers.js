(function (globalScope) {
  function resolveCameraFailure(error, isSecureContext) {
    const denied = error?.name === 'NotAllowedError' || error?.name === 'SecurityError';
    const unavailable = error?.name === 'NotFoundError' || error?.name === 'DevicesNotFoundError' || error?.name === 'OverconstrainedError';
    if (denied) {
      return {
        title: 'CÂMERA BLOQUEADA',
        detail: 'Permita acesso ou use o upload manual',
        message: 'Permita o acesso à câmera para continuar.'
      };
    }
    if (!isSecureContext) {
      return {
        title: 'CONTEXTO INSEGURO',
        detail: 'Abra a página em HTTPS ou localhost',
        message: 'Abra a página em HTTPS ou localhost para usar a câmera.'
      };
    }
    if (unavailable) {
      return {
        title: 'CÂMERA INDISPONÍVEL',
        detail: 'Nenhum dispositivo compatível foi encontrado',
        message: 'Não encontrei uma câmera disponível neste dispositivo.'
      };
    }
    return {
      title: 'CÂMERA INDISPONÍVEL',
      detail: 'Tente novamente ou use o upload manual',
      message: 'Não foi possível iniciar a câmera neste dispositivo.'
    };
  }

  function computeResumeIntent(smartshotAtivo, hasCameraStream) {
    return Boolean(smartshotAtivo && hasCameraStream);
  }

  function shouldRestartCamera(visibilityState, shouldResumeCamera, smartshotAtivo, hasCameraStream) {
    return visibilityState === 'visible' && Boolean(shouldResumeCamera && smartshotAtivo && !hasCameraStream);
  }

  function bindCameraControls(shutterButtons, flipButtons, onCapture, onFlip) {
    shutterButtons.forEach((button) => button.addEventListener('click', onCapture));
    flipButtons.forEach((button) => button.addEventListener('click', onFlip));
  }

  const api = {
    resolveCameraFailure,
    computeResumeIntent,
    shouldRestartCamera,
    bindCameraControls
  };

  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  globalScope.smartShotCameraHelpers = api;
})(typeof window !== 'undefined' ? window : globalThis);
