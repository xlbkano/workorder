package com.workorder.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatsVo {

    private long total;
    private long pendingCount;
    private long processingCount;
    private long approvedCount;
    private long rejectedCount;
    private long withdrawnCount;
    private List<StatItem> byType;
    private List<StatItem> byDept;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StatItem {
        private String name;
        private long count;
    }
}
