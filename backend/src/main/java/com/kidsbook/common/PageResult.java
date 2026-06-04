package com.kidsbook.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class PageResult<T> {
    private List<T> records;
    private long total;
    private int page;
    private int size;
    private int pages;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> extra;

    public static <T> PageResult<T> of(IPage<T> iPage) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(iPage.getRecords());
        result.setTotal(iPage.getTotal());
        result.setPage((int) iPage.getCurrent());
        result.setSize((int) iPage.getSize());
        result.setPages((int) iPage.getPages());
        return result;
    }

    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records != null ? records : Collections.emptyList());
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setPages(size > 0 ? (int) Math.ceil((double) total / size) : 0);
        return result;
    }

    public static <T> PageResult<T> empty(int page, int size) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(Collections.emptyList());
        result.setTotal(0);
        result.setPage(page);
        result.setSize(size);
        result.setPages(0);
        return result;
    }

    public PageResult<T> withExtra(String key, Object value) {
        if (this.extra == null) {
            this.extra = new HashMap<>();
        }
        this.extra.put(key, value);
        return this;
    }
}
