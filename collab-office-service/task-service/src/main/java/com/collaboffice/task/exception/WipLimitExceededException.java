package com.collaboffice.task.exception;

/**
 * WIP 在制品限制超出异常
 * 错误码：WIP_LIMIT_EXCEEDED
 */
public class WipLimitExceededException extends com.collaboffice.common.exception.BusinessException {
    private final Long columnId;
    private final int currentCount;
    private final int wipLimit;

    public WipLimitExceededException(Long columnId, int currentCount, int wipLimit) {
        super(409,String.format("WIP_LIMIT_EXCEEDED: 看板列 [%d] 当前任务数 %d 已达到上限 %d",
                columnId, currentCount, wipLimit));
        this.columnId = columnId;
        this.currentCount = currentCount;
        this.wipLimit = wipLimit;
    }

    public Long getColumnId() { return columnId; }
    public int getCurrentCount() { return currentCount; }
    public int getWipLimit() { return wipLimit; }
}
