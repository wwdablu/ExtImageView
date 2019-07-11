# ExtImageView
Android library allowing cropping of images while displaying them to the user. It allows two (2) forms of cropping:  
* Defined (Rectangle)  
* Freeform (any custom shape)  

## Gradle  
```  
maven { url 'https://jitpack.io' }

dependencies {
    implementation 'com.github.wwdablu:ExtImageView:x.y.z'
}
```  

## Usage  
You can define it in XML as follows:

* Defined  
```  
<com.wwdablu.soumya.extimageview.rect.ExtRectImageView
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:id="@+id/iv_display"
    />
```  
* Freeform  
```
<com.wwdablu.soumya.extimageview.free.ExtFreeImageView
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:id="@+id/iv_display_free"
    />
```  

Once the layout has been inflated we need to pass the actual bitmap which will be used for cropping. For example:  
```
BitmapFactory.Options options = new BitmapFactory.Options();
options.inScaled = false;
options.inDensity = 0;
options.inTargetDensity = 0;
Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sample);

extImageView.setImageBitmap(bitmap);
```  

## Crop(ping)  
To crop the image after selection, we simply need to call the crop method. In here we pass a Result<Void> as a callback to get notified once the crop process is completed or if an exception is generated.  
```
extImageView.crop(resultHandler);
```  

To get the cropped image, we need to call `getCroppedBitmap` which will then return the bitmap. For example:  
```
extFreeImageView.getCroppedBitmap(new Result<Bitmap>() {
    @Override
    public void onComplete(Bitmap data) {
        runOnUiThread(() -> {

            findViewById(R.id.iv_display_free).setVisibility(View.GONE);
            findViewById(R.id.iv_display_cropped).setVisibility(View.VISIBLE);

            ((AppCompatImageView) findViewById(R.id.iv_display_cropped))
                    .setImageBitmap(data);
        });
    }

    @Override
    public void onError(Throwable throwable) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this,
                "Could not get cropped bitmap" + throwable.getMessage(),
                Toast.LENGTH_SHORT).show());
    }
});
```  

## Note  
It creates temporary bitmap files in the cache folder for the crop process. Once the view is detached from the window, these files are removed. This is done automatically. But if the app crashes or in any other scenario detach is not called then these files will linger. If you want you can explicitly remove files from the cache folder with the name ending with _o.png. 
