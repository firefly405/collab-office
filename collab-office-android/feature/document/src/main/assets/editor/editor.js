// Quill, Sheet, Mindmap, Whiteboard 编辑器核心逻辑
import * as Y from './yjs.mjs';

// Base64 helper methods
function bytesToBase64(bytes) {
    let binary = '';
    const len = bytes.byteLength;
    for (let i = 0; i < len; i++) {
        binary += String.fromCharCode(bytes[i]);
    }
    return window.btoa(binary);
}

function base64ToBytes(base64) {
    const binaryString = window.atob(base64);
    const len = binaryString.length;
    const bytes = new Uint8Array(len);
    for (let i = 0; i < len; i++) {
        bytes[i] = binaryString.charCodeAt(i);
    }
    return bytes;
}

// QuillBinding Class for CRDT Doc collaboration
class QuillBinding {
    constructor (type, quill) {
        const doc = type.doc;
        this.type = type;
        this.doc = doc;
        this.quill = quill;
        this._negatedUsedFormats = {};

        this._typeObserver = event => {
            if (event.transaction.origin !== this) {
                const eventDelta = event.delta;
                const delta = [];
                for (let i = 0; i < eventDelta.length; i++) {
                    const d = eventDelta[i];
                    if (d.insert !== undefined) {
                        delta.push(Object.assign({}, d, { attributes: Object.assign({}, this._negatedUsedFormats, d.attributes || {}) }));
                    } else {
                        delta.push(d);
                    }
                }
                quill.updateContents(delta, this);
            }
        };
        type.observe(this._typeObserver);

        this._quillObserver = (eventType, delta, state, origin) => {
            if (delta && delta.ops) {
                const ops = delta.ops;
                ops.forEach(op => {
                    if (op.attributes !== undefined) {
                        for (const key in op.attributes) {
                            if (this._negatedUsedFormats[key] === undefined) {
                                this._negatedUsedFormats[key] = false;
                            }
                        }
                    }
                });
                if (origin !== this) {
                    doc.transact(() => {
                        type.applyDelta(ops);
                    }, this);
                }
            }
        };
        quill.on('editor-change', this._quillObserver);

        // Init quill contents from Yjs state
        quill.setContents(type.toDelta(), this);
    }

    destroy () {
        this.type.unobserve(this._typeObserver);
        this.quill.off('editor-change', this._quillObserver);
    }
}

(function() {
    'use strict';

    // Global active state
    window.activeDocType = 1; // 1=文档 2=表格 3=思维导图 4=白板
    let isApplyingRemote = false;

    // ==================== 1. 富文本 (Quill) 初始化 ====================
    window.quill = new Quill('#editor', {
        theme: 'snow',
        modules: {
            toolbar: '#toolbar'
        },
        placeholder: '开始编辑...',
    });

    quill.on('selection-change', function(range, oldRange, source) {
        if (range && source === 'user' && window.activeDocType === 1) {
            if (window.AndroidBridge) {
                window.AndroidBridge.onCursorChange(JSON.stringify({
                    index: range.index,
                    length: range.length
                }));
            }
        }
    });

    // ==================== 2. 表格 (Sheet) 模块 ====================
    window.sheetData = {};
    window.sheetStyles = {};
    window.selectedSheetCell = null;

    const SheetAPI = {
        init: function() {
            const grid = document.getElementById('sheet-grid');
            grid.innerHTML = '';
            
            // Corner cell
            const corner = document.createElement('div');
            corner.className = 'sheet-header-cell sheet-corner';
            grid.appendChild(corner);

            // Columns headers A-Z
            for (let c = 0; c < 26; c++) {
                const header = document.createElement('div');
                header.className = 'sheet-header-cell col-h';
                header.innerText = String.fromCharCode(65 + c);
                grid.appendChild(header);
            }

            // Rows headers + Data cells
            for (let r = 1; r <= 50; r++) {
                const rowHeader = document.createElement('div');
                rowHeader.className = 'sheet-header-cell row-h';
                rowHeader.innerText = r;
                grid.appendChild(rowHeader);

                for (let c = 0; c < 26; c++) {
                    const colLetter = String.fromCharCode(65 + c);
                    const cellKey = `${colLetter}${r}`;
                    const cell = document.createElement('div');
                    cell.className = 'sheet-data-cell';
                    cell.id = `sheet-cell-${cellKey}`;
                    cell.dataset.cell = cellKey;
                    cell.addEventListener('click', () => this.selectCell(cellKey));
                    cell.addEventListener('dblclick', () => this.editCell(cellKey));
                    grid.appendChild(cell);
                }
            }

            // Setup Formula Bar Enter Event
            const formulaInput = document.getElementById('sheet-formula-input');
            formulaInput.onkeyup = (e) => {
                if (e.key === 'Enter' && window.selectedSheetCell) {
                    this.commitCell(window.selectedSheetCell, formulaInput.value);
                }
            };
        },

        selectCell: function(cellKey) {
            if (window.selectedSheetCell) {
                const prev = document.getElementById(`sheet-cell-${window.selectedSheetCell}`);
                if (prev) prev.classList.remove('selected');
            }
            window.selectedSheetCell = cellKey;
            const curr = document.getElementById(`sheet-cell-${cellKey}`);
            if (curr) curr.classList.add('selected');
            document.getElementById('sheet-active-cell-addr').innerText = cellKey;
            
            const formulaInput = document.getElementById('sheet-formula-input');
            formulaInput.value = window.sheetData[cellKey] || '';
        },

        editCell: function(cellKey) {
            const cellEl = document.getElementById(`sheet-cell-${cellKey}`);
            if (!cellEl) return;
            cellEl.innerHTML = '';
            
            const input = document.createElement('input');
            input.className = 'sheet-cell-edit-input';
            input.value = window.sheetData[cellKey] || '';
            cellEl.appendChild(input);
            input.focus();

            const finishEdit = () => {
                const val = input.value;
                this.commitCell(cellKey, val);
            };

            input.onblur = finishEdit;
            input.onkeyup = (e) => {
                if (e.key === 'Enter') finishEdit();
            };
        },

        commitCell: function(cellKey, val, isLocal = true) {
            window.sheetData[cellKey] = val;
            const cellEl = document.getElementById(`sheet-cell-${cellKey}`);
            if (cellEl) {
                cellEl.innerText = val;
                this.applyCellStyles(cellKey, cellEl);
            }
            if (isLocal) {
                // Send update
                const op = { cell: cellKey, value: val };
                window.AndroidBridge?.onLocalOperation(JSON.stringify(op));
                window.AndroidBridge?.onContentChanged(JSON.stringify(window.sheetData));
            }
        },

        toggleStyle: function(styleName) {
            if (!window.selectedSheetCell) return;
            const cellKey = window.selectedSheetCell;
            if (!window.sheetStyles[cellKey]) window.sheetStyles[cellKey] = {};
            window.sheetStyles[cellKey][styleName] = !window.sheetStyles[cellKey][styleName];
            
            const cellEl = document.getElementById(`sheet-cell-${cellKey}`);
            this.applyCellStyles(cellKey, cellEl);

            // Trigger sync
            this.commitCell(cellKey, window.sheetData[cellKey] || '');
        },

        applyCellStyles: function(cellKey, cellEl) {
            if (!cellEl) return;
            const styles = window.sheetStyles[cellKey] || {};
            cellEl.style.fontWeight = styles.bold ? 'bold' : 'normal';
            cellEl.style.fontStyle = styles.italic ? 'italic' : 'normal';
        },

        clearCell: function() {
            if (window.selectedSheetCell) {
                this.commitCell(window.selectedSheetCell, '');
            }
        },

        exportCSV: function() {
            let csv = '';
            for (let r = 1; r <= 50; r++) {
                let row = [];
                for (let c = 0; c < 26; c++) {
                    const cellKey = `${String.fromCharCode(65 + c)}${r}`;
                    row.push(window.sheetData[cellKey] || '');
                }
                csv += row.join(',') + '\n';
            }
            const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'sheet.csv';
            a.click();
            URL.revokeObjectURL(url);
        }
    };
    window.SheetAPI = SheetAPI;

    // ==================== 3. 思维导图 (Mindmap) 模块 ====================
    window.mindNodes = [];
    window.selectedMindNodeId = null;
    let mindmapTransform = { x: 0, y: 0 };
    let dragStart = null;

    const MindmapAPI = {
        init: function() {
            const svg = document.getElementById('mindmap-svg');
            
            // Drag mindmap background to pan
            svg.addEventListener('mousedown', (e) => {
                if (e.target === svg) {
                    dragStart = { x: e.clientX - mindmapTransform.x, y: e.clientY - mindmapTransform.y };
                }
            });
            svg.addEventListener('mousemove', (e) => {
                if (dragStart) {
                    mindmapTransform.x = e.clientX - dragStart.x;
                    mindmapTransform.y = e.clientY - dragStart.y;
                    this.updateViewbox();
                }
            });
            svg.addEventListener('mouseup', () => { dragStart = null; });
            
            // Touch gestures for mobile device
            svg.addEventListener('touchstart', (e) => {
                if (e.target === svg && e.touches.length === 1) {
                    dragStart = { x: e.touches[0].clientX - mindmapTransform.x, y: e.touches[0].clientY - mindmapTransform.y };
                }
            });
            svg.addEventListener('touchmove', (e) => {
                if (dragStart && e.touches.length === 1) {
                    mindmapTransform.x = e.touches[0].clientX - dragStart.x;
                    mindmapTransform.y = e.touches[0].clientY - dragStart.y;
                    this.updateViewbox();
                }
            });
            svg.addEventListener('touchend', () => { dragStart = null; });
        },

        updateViewbox: function() {
            const svg = document.getElementById('mindmap-svg');
            svg.setAttribute('viewBox', `${-mindmapTransform.x} ${-mindmapTransform.y} 1000 700`);
        },

        reCenter: function() {
            mindmapTransform = { x: 0, y: 0 };
            this.updateViewbox();
        },

        draw: function() {
            const svg = document.getElementById('mindmap-svg');
            
            // Clear connectors and groups
            svg.innerHTML = '';

            // Draw connection lines
            window.mindNodes.forEach(node => {
                if (node.parentId !== null) {
                    const parent = window.mindNodes.find(n => n.id === node.parentId);
                    if (parent) {
                        const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
                        const startX = parent.x + 80;
                        const startY = parent.y + 20;
                        const endX = node.x - 80;
                        const endY = node.y + 20;
                        // Draw horizontal curve
                        const d = `M ${startX} ${startY} C ${(startX+endX)/2} ${startY}, ${(startX+endX)/2} ${endY}, ${endX} ${endY}`;
                        path.setAttribute('d', d);
                        path.setAttribute('stroke', node.color || '#0050d6');
                        path.setAttribute('stroke-width', '2');
                        path.setAttribute('fill', 'none');
                        svg.appendChild(path);
                    }
                }
            });

            // Draw node shapes
            window.mindNodes.forEach(node => {
                const g = document.createElementNS('http://www.w3.org/2000/svg', 'g');
                g.setAttribute('transform', `translate(${node.x}, ${node.y})`);
                g.setAttribute('class', 'mindmap-node');
                
                const rect = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
                rect.setAttribute('x', '-80');
                rect.setAttribute('y', '0');
                rect.setAttribute('width', '160');
                rect.setAttribute('height', '40');
                rect.setAttribute('rx', '6');
                rect.setAttribute('ry', '6');
                
                const isSelected = (window.selectedMindNodeId === node.id);
                rect.setAttribute('fill', isSelected ? '#e8f4ff' : '#ffffff');
                rect.setAttribute('stroke', isSelected ? '#0050d6' : (node.color || '#94a3b8'));
                rect.setAttribute('stroke-width', isSelected ? '2.5' : '1.5');
                g.appendChild(rect);

                const text = document.createElementNS('http://www.w3.org/2000/svg', 'text');
                text.setAttribute('x', '0');
                text.setAttribute('y', '24');
                text.setAttribute('class', 'mindmap-node-text');
                text.textContent = node.content;
                g.appendChild(text);

                // Attach events
                g.addEventListener('click', (e) => {
                    e.stopPropagation();
                    this.selectNode(node.id);
                });
                g.addEventListener('dblclick', (e) => {
                    e.stopPropagation();
                    this.editNode(node.id);
                });

                // Simple drag layout behavior
                this.setupDrag(g, node);

                svg.appendChild(g);
            });
        },

        selectNode: function(id) {
            window.selectedMindNodeId = id;
            this.draw();
        },

        setupDrag: function(el, node) {
            let offset = null;
            const startDrag = (clientX, clientY) => {
                offset = { x: clientX - node.x, y: clientY - node.y };
            };
            const moveDrag = (clientX, clientY) => {
                if (offset) {
                    node.x = clientX - offset.x;
                    node.y = clientY - offset.y;
                    this.draw();
                }
            };
            const stopDrag = () => {
                if (offset) {
                    offset = null;
                    // Broadcast move
                    window.AndroidBridge?.onLocalOperation(JSON.stringify({
                        type: 'mind_move',
                        id: node.id,
                        x: node.x,
                        y: node.y
                    }));
                    window.AndroidBridge?.onContentChanged(JSON.stringify(window.mindNodes));
                }
            };

            el.addEventListener('mousedown', (e) => {
                e.stopPropagation();
                startDrag(e.clientX, e.clientY);
            });
            document.addEventListener('mousemove', (e) => moveDrag(e.clientX, e.clientY));
            document.addEventListener('mouseup', stopDrag);

            el.addEventListener('touchstart', (e) => {
                e.stopPropagation();
                if (e.touches.length === 1) {
                    startDrag(e.touches[0].clientX, e.touches[0].clientY);
                }
            });
            document.addEventListener('touchmove', (e) => {
                if (e.touches.length === 1) moveDrag(e.touches[0].clientX, e.touches[0].clientY);
            });
            document.addEventListener('touchend', stopDrag);
        },

        editNode: function(id) {
            const node = window.mindNodes.find(n => n.id === id);
            if (!node) return;
            const val = prompt('修改节点内容', node.content);
            if (val !== null && val.trim() !== '') {
                node.content = val;
                this.draw();
                // Send change
                window.AndroidBridge?.onLocalOperation(JSON.stringify({
                    type: 'mind_update_text',
                    id: id,
                    content: val
                }));
                window.AndroidBridge?.onContentChanged(JSON.stringify(window.mindNodes));
            }
        },

        addChildNode: function() {
            if (!window.selectedMindNodeId) return;
            const parent = window.mindNodes.find(n => n.id === window.selectedMindNodeId);
            if (!parent) return;
            
            const nextId = window.mindNodes.length > 0 ? Math.max(...window.mindNodes.map(n => n.id)) + 1 : 1;
            const colors = ['#0050d6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'];
            const newNode = {
                id: nextId,
                parentId: parent.id,
                content: '新节点',
                x: parent.x + 220,
                y: parent.y + (window.mindNodes.filter(n => n.parentId === parent.id).length - 1) * 60,
                color: colors[nextId % colors.length]
            };
            window.mindNodes.push(newNode);
            this.draw();

            // Send sync
            window.AndroidBridge?.onLocalOperation(JSON.stringify({
                type: 'mind_add',
                node: newNode
            }));
            window.AndroidBridge?.onContentChanged(JSON.stringify(window.mindNodes));
        },

        addSiblingNode: function() {
            if (!window.selectedMindNodeId) return;
            const sibling = window.mindNodes.find(n => n.id === window.selectedMindNodeId);
            if (!sibling || sibling.parentId === null) return; // Root node cannot have sibling
            
            const parent = window.mindNodes.find(n => n.id === sibling.parentId);
            const nextId = window.mindNodes.length > 0 ? Math.max(...window.mindNodes.map(n => n.id)) + 1 : 1;
            const colors = ['#0050d6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'];
            const newNode = {
                id: nextId,
                parentId: parent.id,
                content: '新节点',
                x: sibling.x,
                y: sibling.y + 60,
                color: colors[nextId % colors.length]
            };
            window.mindNodes.push(newNode);
            this.draw();

            // Send sync
            window.AndroidBridge?.onLocalOperation(JSON.stringify({
                type: 'mind_add',
                node: newNode
            }));
            window.AndroidBridge?.onContentChanged(JSON.stringify(window.mindNodes));
        },

        deleteSelectedNode: function() {
            if (!window.selectedMindNodeId || window.selectedMindNodeId === 1) return; // Root cannot be deleted
            const removeIds = new Set();
            const collect = (id) => {
                removeIds.add(id);
                window.mindNodes.filter(n => n.parentId === id).forEach(c => collect(c.id));
            };
            collect(window.selectedMindNodeId);
            
            window.mindNodes = window.mindNodes.filter(n => !removeIds.has(n.id));
            window.selectedMindNodeId = null;
            this.draw();

            // Send sync
            window.AndroidBridge?.onLocalOperation(JSON.stringify({
                type: 'mind_delete',
                id: window.selectedMindNodeId
            }));
            window.AndroidBridge?.onContentChanged(JSON.stringify(window.mindNodes));
        }
    };
    window.MindmapAPI = MindmapAPI;

    // ==================== 4. 白板 (Whiteboard) 模块 ====================
    window.wbHistory = [];
    window.wbRedoStack = [];
    let currentStroke = null;
    let wbCtx = null;
    let selectedWbTool = 'pen';
    let wbColor = '#333333';
    let wbWidth = 3;
    let isDrawing = false;
    let startX = 0, startY = 0;
    let wbSnapshot = null;

    const WhiteboardAPI = {
        init: function() {
            const canvas = document.getElementById('whiteboard-canvas');
            if (!canvas) return;
            wbCtx = canvas.getContext('2d');
            wbCtx.lineCap = 'round';
            wbCtx.lineJoin = 'round';
            this.clearBoard(false);

            // Event bindings
            const startDraw = (clientX, clientY) => {
                const rect = canvas.getBoundingClientRect();
                startX = clientX - rect.left;
                startY = clientY - rect.top;
                isDrawing = true;

                if (selectedWbTool === 'pen' || selectedWbTool === 'eraser') {
                    this.saveSnapshot();
                    const color = selectedWbTool === 'eraser' ? '#ffffff' : wbColor;
                    const width = selectedWbTool === 'eraser' ? wbWidth * 4 : wbWidth;
                    currentStroke = {
                        tool: selectedWbTool,
                        color,
                        width,
                        points: [[startX, startY]]
                    };
                    wbCtx.beginPath();
                    wbCtx.strokeStyle = color;
                    wbCtx.lineWidth = width;
                    wbCtx.moveTo(startX, startY);
                } else {
                    this.saveSnapshot();
                }
            };

            const moveDraw = (clientX, clientY) => {
                if (!isDrawing) return;
                const rect = canvas.getBoundingClientRect();
                const x = clientX - rect.left;
                const y = clientY - rect.top;

                if ((selectedWbTool === 'pen' || selectedWbTool === 'eraser') && currentStroke) {
                    currentStroke.points.push([x, y]);
                    wbCtx.lineTo(x, y);
                    wbCtx.stroke();
                } else if (selectedWbTool === 'rect' || selectedWbTool === 'circle' || selectedWbTool === 'line') {
                    this.restoreSnapshot();
                    wbCtx.strokeStyle = wbColor;
                    wbCtx.lineWidth = wbWidth;
                    wbCtx.beginPath();

                    if (selectedWbTool === 'rect') {
                        wbCtx.strokeRect(startX, startY, x - startX, y - startY);
                    } else if (selectedWbTool === 'circle') {
                        const r = Math.sqrt(Math.pow(x - startX, 2) + Math.pow(y - startY, 2));
                        wbCtx.arc(startX, startY, r, 0, 2 * Math.PI);
                        wbCtx.stroke();
                    } else if (selectedWbTool === 'line') {
                        wbCtx.moveTo(startX, startY);
                        wbCtx.lineTo(x, y);
                        wbCtx.stroke();
                    }
                }
            };

            const stopDraw = (clientX, clientY) => {
                if (!isDrawing) return;
                isDrawing = false;
                const rect = canvas.getBoundingClientRect();
                const x = clientX ? clientX - rect.left : startX;
                const y = clientY ? clientY - rect.top : startY;

                if (currentStroke && (selectedWbTool === 'pen' || selectedWbTool === 'eraser')) {
                    window.wbHistory.push(currentStroke);
                    window.wbRedoStack = [];
                    // Sync
                    window.AndroidBridge?.onLocalOperation(JSON.stringify({
                        type: 'wb_draw',
                        stroke: currentStroke
                    }));
                } else if (selectedWbTool === 'rect' || selectedWbTool === 'circle' || selectedWbTool === 'line') {
                    const finalShape = {
                        tool: selectedWbTool,
                        color: wbColor,
                        width: wbWidth,
                        x: startX,
                        y: startY,
                        w: x,
                        h: y
                    };
                    window.wbHistory.push(finalShape);
                    window.wbRedoStack = [];
                    // Sync
                    window.AndroidBridge?.onLocalOperation(JSON.stringify({
                        type: 'wb_draw',
                        stroke: finalShape
                    }));
                }
                currentStroke = null;
                window.AndroidBridge?.onContentChanged(JSON.stringify(window.wbHistory));
            };

            // Mouse events
            canvas.addEventListener('mousedown', (e) => startDraw(e.clientX, e.clientY));
            canvas.addEventListener('mousemove', (e) => moveDraw(e.clientX, e.clientY));
            canvas.addEventListener('mouseup', (e) => stopDraw(e.clientX, e.clientY));
            canvas.addEventListener('mouseleave', () => stopDraw());

            // Touch events for mobile screens
            canvas.addEventListener('touchstart', (e) => {
                if (e.touches.length === 1) {
                    startDraw(e.touches[0].clientX, e.touches[0].clientY);
                }
            });
            canvas.addEventListener('touchmove', (e) => {
                if (e.touches.length === 1) {
                    moveDraw(e.touches[0].clientX, e.touches[0].clientY);
                }
            });
            canvas.addEventListener('touchend', (e) => {
                stopDraw();
            });
        },

        saveSnapshot: function() {
            const canvas = document.getElementById('whiteboard-canvas');
            wbSnapshot = wbCtx.getImageData(0, 0, canvas.width, canvas.height);
        },

        restoreSnapshot: function() {
            if (wbSnapshot) {
                wbCtx.putImageData(wbSnapshot, 0, 0);
            }
        },

        setTool: function(tool) {
            selectedWbTool = tool;
            document.querySelectorAll('#whiteboard-toolbar .btn').forEach(btn => {
                btn.classList.remove('active');
            });
            document.getElementById(`wb-tool-${tool}`)?.classList.add('active');
        },

        setColor: function(color) {
            wbColor = color;
        },

        setWidth: function(w) {
            wbWidth = parseInt(w);
        },

        clearBoard: function(isLocal = true) {
            const canvas = document.getElementById('whiteboard-canvas');
            if (!wbCtx || !canvas) return;
            wbCtx.clearRect(0, 0, canvas.width, canvas.height);
            wbCtx.fillStyle = '#ffffff';
            wbCtx.fillRect(0, 0, canvas.width, canvas.height);
            if (isLocal) {
                window.wbHistory = [];
                window.wbRedoStack = [];
                // Broadcast clear
                window.AndroidBridge?.onLocalOperation(JSON.stringify({ type: 'wb_clear' }));
                window.AndroidBridge?.onContentChanged(JSON.stringify([]));
            }
        },

        undo: function() {
            if (window.wbHistory.length === 0) return;
            window.wbRedoStack.push(window.wbHistory.pop());
            this.redraw();
            window.AndroidBridge?.onContentChanged(JSON.stringify(window.wbHistory));
            window.AndroidBridge?.onLocalOperation(JSON.stringify({ type: 'wb_undo' }));
        },

        redo: function() {
            if (window.wbRedoStack.length === 0) return;
            window.wbHistory.push(window.wbRedoStack.pop());
            this.redraw();
            window.AndroidBridge?.onContentChanged(JSON.stringify(window.wbHistory));
            window.AndroidBridge?.onLocalOperation(JSON.stringify({ type: 'wb_redo' }));
        },

        redraw: function() {
            this.clearBoard(false);
            window.wbHistory.forEach(stroke => {
                wbCtx.strokeStyle = stroke.color;
                wbCtx.lineWidth = stroke.width;
                wbCtx.beginPath();
                if ((stroke.tool === 'pen' || stroke.tool === 'eraser') && stroke.points) {
                    if (stroke.points.length > 0) {
                        wbCtx.moveTo(stroke.points[0][0], stroke.points[0][1]);
                        for (let i = 1; i < stroke.points.length; i++) {
                            wbCtx.lineTo(stroke.points[i][0], stroke.points[i][1]);
                        }
                        wbCtx.stroke();
                    }
                } else if (stroke.tool === 'rect') {
                    wbCtx.strokeRect(stroke.x, stroke.y, stroke.w - stroke.x, stroke.h - stroke.y);
                } else if (stroke.tool === 'circle') {
                    const r = Math.sqrt(Math.pow(stroke.w - stroke.x, 2) + Math.pow(stroke.h - stroke.y, 2));
                    wbCtx.arc(stroke.x, stroke.y, r, 0, 2 * Math.PI);
                    wbCtx.stroke();
                } else if (stroke.tool === 'line') {
                    wbCtx.moveTo(stroke.x, stroke.y);
                    wbCtx.lineTo(stroke.w, stroke.h);
                    wbCtx.stroke();
                }
            });
        },

        clearCanvas: function() {
            if (confirm('确定清空白板吗？')) {
                this.clearBoard(true);
            }
        }
    };
    window.WhiteboardAPI = WhiteboardAPI;

    // ==================== 5. 通用接口层 (API for Android Bridge) ====================
    window.EditorAPI = {
        initEditor: function(type, contentJson) {
            window.activeDocType = type;
            
            // Switch sections
            document.querySelectorAll('.editor-section').forEach(s => s.classList.remove('active'));
            if (type === 1) {
                document.getElementById('doc-section').classList.add('active');
            } else if (type === 2) {
                document.getElementById('sheet-section').classList.add('active');
                SheetAPI.init();
            } else if (type === 3) {
                document.getElementById('mindmap-section').classList.add('active');
                MindmapAPI.init();
            } else if (type === 4) {
                document.getElementById('whiteboard-section').classList.add('active');
                WhiteboardAPI.init();
            }

            this.setContent(contentJson);
        },

        setContent: function(contentJson) {
            if (!contentJson) return;
            try {
                isApplyingRemote = true;
                if (window.activeDocType === 1) {
                    let isHtml = false;
                    let content;
                    try {
                        content = JSON.parse(contentJson);
                        if (!Array.isArray(content) && typeof content !== 'object') {
                            isHtml = true;
                        }
                    } catch (e) {
                        isHtml = true;
                    }

                    if (isHtml) {
                        const delta = quill.clipboard.convert({ html: contentJson });
                        quill.setContents(delta, 'api');
                    } else {
                        quill.setContents(content, 'api');
                    }

                    // Setup Yjs binding
                    if (window.ybinding) {
                        window.ybinding.destroy();
                    }
                    window.ydoc = new Y.Doc();
                    window.ytext = ydoc.getText('quill');
                    
                    const initialOps = quill.getContents().ops;
                    ydoc.transact(() => {
                        ytext.applyDelta(initialOps);
                    }, 'initial');

                    window.ybinding = new QuillBinding(ytext, quill);

                    // Local changes observer
                    ydoc.on('update', (update, origin) => {
                        if (origin !== ybinding && origin !== 'initial' && window.activeDocType === 1) {
                            const base64 = bytesToBase64(update);
                            window.AndroidBridge?.onLocalOperation(JSON.stringify({
                                kind: 'yjs_update',
                                update: base64
                            }));
                            window.AndroidBridge?.onContentChanged(bytesToBase64(Y.encodeStateAsUpdate(ydoc)));
                        }
                    });

                    // Request state sync from online collaborators
                    window.AndroidBridge?.onLocalOperation(JSON.stringify({
                        kind: 'yjs_sync_request',
                        clientId: String(ydoc.clientID)
                    }));
                } else if (window.activeDocType === 2) {
                    window.sheetData = JSON.parse(contentJson);
                    for (const cellKey in window.sheetData) {
                        const cellEl = document.getElementById(`sheet-cell-${cellKey}`);
                        if (cellEl) cellEl.innerText = window.sheetData[cellKey];
                    }
                } else if (window.activeDocType === 3) {
                    window.mindNodes = JSON.parse(contentJson);
                    if (!window.mindNodes || window.mindNodes.length === 0) {
                        window.mindNodes = [{ id: 1, parentId: null, content: '中心主题', x: 400, y: 300, color: '#0050d6' }];
                    }
                    MindmapAPI.draw();
                } else if (window.activeDocType === 4) {
                    window.wbHistory = JSON.parse(contentJson);
                    WhiteboardAPI.redraw();
                }
            } catch (e) {
                console.error('setContent error:', e);
            } finally {
                isApplyingRemote = false;
            }
        },

        applyRemoteOperation: function(opJson) {
            if (!opJson) return;
            try {
                isApplyingRemote = true;
                if (window.activeDocType === 1) {
                    const op = JSON.parse(opJson);
                    if (op.kind === 'yjs_sync_request') {
                        const base64 = bytesToBase64(Y.encodeStateAsUpdate(ydoc));
                        window.AndroidBridge?.onLocalOperation(JSON.stringify({
                            kind: 'yjs_state',
                            update: base64
                        }));
                    } else if (op.kind === 'yjs_state' || op.kind === 'yjs_update') {
                        if (op.update) {
                            const bytes = base64ToBytes(op.update);
                            Y.applyUpdate(ydoc, bytes, 'remote');
                        }
                    }
                } else if (window.activeDocType === 2) {
                    const op = JSON.parse(opJson);
                    if (op.cell) {
                        SheetAPI.commitCell(op.cell, op.value, false);
                    }
                } else if (window.activeDocType === 3) {
                    const op = JSON.parse(opJson);
                    if (op.type === 'mind_add') {
                        if (!window.mindNodes.find(n => n.id === op.node.id)) {
                            window.mindNodes.push(op.node);
                        }
                    } else if (op.type === 'mind_delete') {
                        window.mindNodes = window.mindNodes.filter(n => n.id !== op.id && n.parentId !== op.id);
                    } else if (op.type === 'mind_update_text') {
                        const node = window.mindNodes.find(n => n.id === op.id);
                        if (node) node.content = op.content;
                    } else if (op.type === 'mind_move') {
                        const node = window.mindNodes.find(n => n.id === op.id);
                        if (node) {
                            node.x = op.x;
                            node.y = op.y;
                        }
                    }
                    MindmapAPI.draw();
                } else if (window.activeDocType === 4) {
                    const op = JSON.parse(opJson);
                    if (op.type === 'wb_draw') {
                        window.wbHistory.push(op.stroke);
                        WhiteboardAPI.redraw();
                    } else if (op.type === 'wb_clear') {
                        WhiteboardAPI.clearBoard(false);
                    } else if (op.type === 'wb_undo') {
                        if (window.wbHistory.length > 0) window.wbHistory.pop();
                        WhiteboardAPI.redraw();
                    }
                }
            } catch (e) {
                console.error('applyRemoteOperation error:', e);
            } finally {
                isApplyingRemote = false;
            }
        },

        getContent: function() {
            if (window.activeDocType === 1) {
                return bytesToBase64(Y.encodeStateAsUpdate(ydoc));
            } else if (window.activeDocType === 2) {
                return JSON.stringify(window.sheetData);
            } else if (window.activeDocType === 3) {
                return JSON.stringify(window.mindNodes);
            } else if (window.activeDocType === 4) {
                return JSON.stringify(window.wbHistory);
            }
            return '';
        },

        getText: function() {
            if (window.activeDocType === 1) {
                return quill.getText();
            }
            return '';
        },

        setReadOnly: function(readOnly) {
            if (window.activeDocType === 1) {
                quill.enable(!readOnly);
            }
        },

        showCursor: function(userId, name, color, index, length) {
            if (window.activeDocType !== 1) return;
            removeCursor(userId);
            const bounds = quill.getBounds(index, length || 0);
            if (!bounds) return;

            const cursor = document.createElement('div');
            cursor.className = 'ql-cursor';
            cursor.id = 'cursor-' + userId;
            cursor.style.borderColor = color;
            cursor.style.left = bounds.left + 'px';
            cursor.style.top = bounds.top + 'px';
            cursor.style.height = bounds.height + 'px';

            const nameTag = document.createElement('div');
            nameTag.className = 'ql-cursor-name';
            nameTag.style.backgroundColor = color;
            nameTag.textContent = name;
            cursor.appendChild(nameTag);

            document.querySelector('#editor-container').appendChild(cursor);
            setTimeout(() => removeCursor(userId), 10000);
        },

        removeCursor: function(userId) {
            removeCursor(userId);
        },

        updateCollaborators: function(collaboratorsJson) {
            try {
                const collaborators = JSON.parse(collaboratorsJson);
                const container = document.getElementById('collaborators');
                container.innerHTML = '';
                collaborators.forEach(function(c) {
                    const avatar = document.createElement('div');
                    avatar.className = 'collaborator-avatar';
                    avatar.style.backgroundColor = c.color || '#666';
                    avatar.textContent = (c.name || 'U').charAt(0).toUpperCase();
                    avatar.title = c.name;
                    container.appendChild(avatar);
                });
            } catch (e) {
                console.error('updateCollaborators error:', e);
            }
        }
    };

    function removeCursor(userId) {
        const existing = document.getElementById('cursor-' + userId);
        if (existing) existing.remove();
    }

    // Notify Android that editor is ready
    if (window.AndroidBridge) {
        window.AndroidBridge.onEditorReady();
    }
})();
