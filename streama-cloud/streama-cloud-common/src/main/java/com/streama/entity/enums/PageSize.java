package com.streama.entity.enums;

public enum PageSize {
    SIZE10(10), SIZE15(15), SIZE20(20), SIZE30(30), SIZE50(40), SIZE100(50);

    int size;

    private PageSize(int size) {
        this.size = size;
    }

    public int getSize() {
        return this.size;
    }
}
