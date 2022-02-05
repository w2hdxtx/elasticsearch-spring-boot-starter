package com.leyou.starter.elastic.entity;

import java.util.List;

public class PageInfo<T> {
    private long total;
    private List<T> content;

    public PageInfo() {
    }

    public PageInfo(long total, List<T> content) {
        this.total = total;
        this.content = content;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }
}
