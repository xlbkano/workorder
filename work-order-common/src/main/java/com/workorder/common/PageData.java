package com.workorder.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统一分页返回结构
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageData<T> {

    /** 总记录数 */
    private Long total;
    /** 当前页数据 */
    private List<T> rows;
    /** 游标分页时返回，下一页请求带上该值 */
    private Long nextLastId;
}
