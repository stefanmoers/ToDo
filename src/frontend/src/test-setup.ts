import '@testing-library/jest-dom/vitest'

// jsdom kennt <dialog>, aber nicht immer showModal()/close(); nur ergänzen, falls nötig.
if (typeof HTMLDialogElement !== 'undefined' && typeof HTMLDialogElement.prototype.showModal !== 'function') {
  HTMLDialogElement.prototype.showModal = function () {
    this.setAttribute('open', '')
  }
  HTMLDialogElement.prototype.close = function () {
    this.removeAttribute('open')
  }
}
