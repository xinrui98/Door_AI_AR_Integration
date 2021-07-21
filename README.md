# Final internship project demonstrating the integration of Machine Learning (Object Detection/Image Classification) and Augmented Reality.

## Starting sample project by Google
An [ARCore](https://developers.google.com/ar) sample demonstrating how to use
camera images as an input for machine learning algorithms, and how to use the
results of the inference model to create anchors in the AR scene.

<p align="center">
  
</p>

This sample uses [ML Kit's Object Detection](https://developers.google.com/ml-kit/vision/object-detection)
and (optionally) [Google's Cloud Vision API](https://cloud.google.com/vision/docs/object-localizer)
to infer object labels from camera images.

## Getting Started
To try this app, you'll need the following:

 * An ARCore compatible device running [Google Play Services for AR](https://play.google.com/store/apps/details?id=com.google.ar.core) (ARCore) 1.24 or later
 * Android Studio 4.1 or later
 
### Configure ML Kit's classification model
By default, this sample uses ML Kit's built-in coarse classifier, which is only built for five categories and provides limited information about the detected objects.

For better classification results:

1. Read [Label images with a custom model on Android](https://developers.google.com/ml-kit/vision/object-detection/custom-models/android)
   on ML Kit's documentation website.
2. Modify `MLKitObjectAnalyzer.kt` in `src/main/java/com/google/ar/core/examples/java/ml/mlkit/` to specify a custom model. (I trained a custom Door Image Classification model for this project)

### \[Optional] Configure Google Cloud Vision API
This sample also supports results from [the Google Cloud Vision API](https://cloud.google.com/vision/docs/object-localizer) for even more information on detected objects.

To configure Google Cloud Vision APIs:

1. Follow steps for configuring a Google Cloud project, enabling billing, enabling the API, and enabling a service account on [Set up the Vision API documentation](https://cloud.google.com/vision/docs/setup).
2. Save the resulting service account key file to `app/src/main/res/raw/credentials.json`.

## openGL notes

Implemented
* obj and texture loading
* shaders for obj rendering
* model matrix for affine transformation
* gestures

### Obj and texture loading

In [AppRenderer.kt](app/src/main/java/com/google/ar/core/examples/java/ml/AppRenderer.kt) the line
```kotlin
val objRenderer = ObjRender("obj/door.obj", "obj/door_texture.png", 0.001f)
```
loads the model door.obj with the texture door_texture.png from the folder asset/obj with a size of 0.001f.
And the line
```kotlin
objRenderer.draw(render, modelViewProjectionMatrix)
```
draws the textured model at the label place.

The size 0.001f depends on mesh itself, so you need to adjust it for each 3d model.

### Shaders for obj rendering

The class [ObjRender](app/src/main/java/com/google/ar/core/examples/java/ml/render/ObjRender.kt) uses two new shaders [asset/shaders/obj.vert](app/src/main/assets/shaders/obj.vert) and [asset/shaders/obj.frag](app/src/main/assets/shaders/obj.frag).
This shaders are specific for obj model rendering.
Anyway you don't need to code or define other shaders for a basic rendering with no lighting or shadows.

### Model matrix for affine transformation
The rendering method `onDrawFrame()` in class `AppRenderer`
defines a modelMatrix for affine transformations of translation and scaling.
If you want to add other transformations such as rotation you need to add it in this block
```kotlin
Matrix.setIdentityM(modelMatrix, 0)
Matrix.translateM(modelMatrix, 0, anchor.pose.tx(), anchor.pose.ty(), anchor.pose.tz())
Matrix.scaleM(modelMatrix, 0, objRenderer.size, objRenderer.size, objRenderer.size)
Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
```

### Gestures

Added gestures for translation, scaling and rotation.
The gestures handler is set in `SampleRender.java`
```java
gl30SurfaceView.setTouchListener(new GL30SurfaceView.TouchListener() {

    private final float VELOCITY_FACTOR_SINGLE_TOUCH = 1f/50f;
    private final float VELOCITY_FACTOR_MULTITOUCH = 4f;
    private final float VELOCITY_LIMIT = 0.8f;

    private ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(gl30SurfaceView.getContext(), new GL30SurfaceView.ScaleListener() {

      @Override
      public boolean onScale(ScaleGestureDetector detector) {
        // Don't let the object get too small or too large.
        renderer.onScale(Math.max(0.1f, Math.min(detector.getScaleFactor(), 5.0f)));
        gl30SurfaceView.invalidate();
        return true;
      }
    });

    @Override
    public boolean onTouch(MotionEvent ev) {

      // Let the ScaleGestureDetector inspect all events.
      scaleGestureDetector.onTouchEvent(ev);

      final int action = MotionEventCompat.getActionMasked(ev);

      if (action == MotionEvent.ACTION_MOVE) {

        if (ev.getPointerCount() > 1) {
          // Multitouch event
          VelocityTracker vt = VelocityTracker.obtain();
          vt.addMovement(ev);
          vt.computeCurrentVelocity(1);
          float velX = vt.getXVelocity();
          float velY = vt.getYVelocity();
          if (velX > VELOCITY_LIMIT || velY > VELOCITY_LIMIT || velX < -VELOCITY_LIMIT || velY < -VELOCITY_LIMIT) {
            renderer.onRotation(velX* VELOCITY_FACTOR_MULTITOUCH, velY* VELOCITY_FACTOR_MULTITOUCH);
          }
          vt.recycle();

        } else {
          // Single touch event
          VelocityTracker vt = VelocityTracker.obtain();
          vt.addMovement(ev);
          vt.computeCurrentVelocity(1);
          renderer.onMove(vt.getXVelocity()* VELOCITY_FACTOR_SINGLE_TOUCH, -vt.getYVelocity()* VELOCITY_FACTOR_SINGLE_TOUCH);
          vt.recycle();
        }
      }


      return true;
    }
  });
```

In this handler the line `scaleGestureDetector.onTouchEvent(ev)`
handles the scaling operation. By pinching on the screen the object displayed increases or decreases its size.
The next lines
```java
final int action = MotionEventCompat.getActionMasked(ev);
  if (action == MotionEvent.ACTION_MOVE) {

    if (ev.getPointerCount() > 1) {
      // Multitouch event
      VelocityTracker vt = VelocityTracker.obtain();
      vt.addMovement(ev);
      vt.computeCurrentVelocity(1);
      float velX = vt.getXVelocity();
      float velY = vt.getYVelocity();
      if (velX > VELOCITY_LIMIT || velY > VELOCITY_LIMIT || velX < -VELOCITY_LIMIT || velY < -VELOCITY_LIMIT) {
        renderer.onRotation(velX* VELOCITY_FACTOR_MULTITOUCH, velY* VELOCITY_FACTOR_MULTITOUCH);
      }
      vt.recycle();

    } else {
      // Single touch event
      VelocityTracker vt = VelocityTracker.obtain();
      vt.addMovement(ev);
      vt.computeCurrentVelocity(1);
      renderer.onMove(vt.getXVelocity()* VELOCITY_FACTOR_SINGLE_TOUCH, -vt.getYVelocity()* VELOCITY_FACTOR_SINGLE_TOUCH);
      vt.recycle();
    }
  }
```
handles translation and rotation. By moving one finger the object displayed translates. By fixing one finger on the screen
and move a second finger on the screen the object displayed rotates.

This three functions in `AppRenderer.kt` are invoked by the code above for updating the values defining rotation, translation and scaling.

```kotlin
override fun onMove(posX: Float, posY: Float) {
    objRenderer.mesh.pos[0] += posX
    objRenderer.mesh.pos[1] += posY
    objRenderer.mesh.pos[2] = 0f
}

override fun onScale(scaleFactor: Float) {
    objRenderer.mesh.scale *= scaleFactor
}

override fun onRotation(x: Float, y: Float) {
    objRenderer.mesh.rot[0] += x
    objRenderer.mesh.rot[1] += y
    objRenderer.mesh.rot[2] = 0f
}
```
The translation, rotation and scaling are computed by this lines in `AppRenderer.kt`
```kotlin
Matrix.translateM(
        modelMatrix,
        0,
        anchor.pose.tx() + objRenderer.mesh.pos[0],
        anchor.pose.ty() + objRenderer.mesh.pos[1],
        anchor.pose.tz() + objRenderer.mesh.pos[2])
Matrix.rotateM(modelMatrix, 0, objRenderer.mesh.rot[0], 0f, 1f, 0f)
//Matrix.rotateM(modelMatrix, 0, objRenderer.mesh.rot[1], 1f, 0f, 0f)
Matrix.scaleM(modelMatrix, 0, objRenderer.mesh.scale, objRenderer.mesh.scale, objRenderer.mesh.scale)
```


## License

    Copyright 2021 Google LLC

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.