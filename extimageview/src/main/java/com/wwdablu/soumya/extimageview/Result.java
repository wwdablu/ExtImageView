package com.wwdablu.soumya.extimageview;

public interface Result<T> {
    void onComplete(T data);
    void onError(Throwable throwable);
}
