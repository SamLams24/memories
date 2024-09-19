package com.example.memories;

import androidx.annotation.NonNull;

public interface MemoryViewHolder {
    void onViewDetachedFromWindow();

    void onViewDetachedFromWindow(@NonNull MemoryAdapter.MemoryViewHolder holder);
}
