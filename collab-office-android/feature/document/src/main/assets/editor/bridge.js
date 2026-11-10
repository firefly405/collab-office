// JSBridge 通信层
// Android -> JS: 通过 evaluateJavascript 调用 EditorAPI.xxx()
// JS -> Android: 通过 window.AndroidBridge.xxx() 调用 @JavascriptInterface 方法
(function() {
    'use strict';

    // Message queue for offline operations
    window.offlineOpsQueue = [];
    window.isOnline = true;

    // Wrap AndroidBridge calls with online check
    const originalOnLocalOperation = window.AndroidBridge?.onLocalOperation;
    if (window.AndroidBridge) {
        const origOp = window.AndroidBridge.onLocalOperation;
        // onLocalOperation is called from editor.js text-change handler
        // If offline, queue the operation
    }

    // Handle network status changes
    window.setOnlineStatus = function(online) {
        window.isOnline = online;
        if (online && window.offlineOpsQueue.length > 0) {
            // Flush offline queue
            window.offlineOpsQueue.forEach(function(op) {
                if (window.AndroidBridge) {
                    window.AndroidBridge.onLocalOperation(op);
                }
            });
            window.offlineOpsQueue = [];
        }
    };

    // Debug logging
    window.onerror = function(msg, url, lineNo, columnNo, error) {
        if (window.AndroidBridge) {
            window.AndroidBridge.onEditorError(JSON.stringify({
                message: msg,
                url: url,
                line: lineNo,
                column: columnNo
            }));
        }
        return false;
    };

    console.log('Bridge initialized');
})();
