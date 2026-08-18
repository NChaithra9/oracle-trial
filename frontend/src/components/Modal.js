import React, { useEffect } from "react";
import { X } from "lucide-react";

/**
 * Generic centered modal: dark overlay + panel with a title and close
 * button. Used for both the document details panel and the About panel so
 * they share one consistent look.
 */
export default function Modal({ title, onClose, children }) {
  // Let Escape close the modal, like any standard dialog.
  useEffect(() => {
    function handleKeyDown(event) {
      if (event.key === "Escape") onClose();
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>{title}</h2>
          <button type="button" className="modal-close" onClick={onClose} aria-label="Close">
            <X size={18} strokeWidth={2} />
          </button>
        </div>
        <div className="modal-body">{children}</div>
      </div>
    </div>
  );
}
