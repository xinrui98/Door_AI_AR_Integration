/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.core.examples.java.common.samplerender;

import android.content.res.AssetManager;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;

import androidx.core.view.MotionEventCompat;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.view.MotionEvent.INVALID_POINTER_ID;

/** A SampleRender context. */
public class SampleRender {
  private static final String TAG = SampleRender.class.getSimpleName();

  private final AssetManager assetManager;

  private int viewportWidth = 1;
  private int viewportHeight = 1;

  // The ‘active pointer’ is the one currently moving our object.
  private int mActivePointerId = INVALID_POINTER_ID;

  /**
   * Constructs a SampleRender object and instantiates GLSurfaceView parameters.
   *
   * @param gl30SurfaceView Android GLSurfaceView
   * @param renderer Renderer implementation to receive callbacks
   * @param assetManager AssetManager for loading Android resources
   */
  public SampleRender(GL30SurfaceView gl30SurfaceView, Renderer renderer, AssetManager assetManager) {
    this.assetManager = assetManager;
    gl30SurfaceView.setPreserveEGLContextOnPause(true);
    gl30SurfaceView.setEGLContextClientVersion(3);
    gl30SurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
    gl30SurfaceView.setRenderer(
            new GLSurfaceView.Renderer() {
              @Override
              public void onSurfaceCreated(GL10 gl, EGLConfig config) {
                GLES30.glEnable(GLES30.GL_BLEND);
                GLError.maybeThrowGLException("Failed to enable blending", "glEnable");
                renderer.onSurfaceCreated(SampleRender.this);
              }

              @Override
              public void onSurfaceChanged(GL10 gl, int w, int h) {
                viewportWidth = w;
                viewportHeight = h;
                renderer.onSurfaceChanged(SampleRender.this, w, h);
              }

              @Override
              public void onDrawFrame(GL10 gl) {
                clear(/*framebuffer=*/ null, 0f, 0f, 0f, 1f);
                renderer.onDrawFrame(SampleRender.this);
              }
            });
    gl30SurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    gl30SurfaceView.setWillNotDraw(false);

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
  }

  /** Draw a {@link Mesh} with the specified {@link Shader}. */
  public void draw(Mesh mesh, Shader shader) {
    draw(mesh, shader, /*framebuffer=*/ null);
  }

  /**
   * Draw a {@link Mesh} with the specified {@link Shader} to the given {@link Framebuffer}.
   *
   * <p>The {@code framebuffer} argument may be null, in which case the default framebuffer is used.
   */
  public void draw(Mesh mesh, Shader shader, Framebuffer framebuffer) {
    useFramebuffer(framebuffer);
    shader.lowLevelUse();
    mesh.lowLevelDraw();
  }

  /**
   * Clear the given framebuffer.
   *
   * <p>The {@code framebuffer} argument may be null, in which case the default framebuffer is
   * cleared.
   */
  public void clear(Framebuffer framebuffer, float r, float g, float b, float a) {
    useFramebuffer(framebuffer);
    GLES30.glClearColor(r, g, b, a);
    GLError.maybeThrowGLException("Failed to set clear color", "glClearColor");
    GLES30.glDepthMask(true);
    GLError.maybeThrowGLException("Failed to set depth write mask", "glDepthMask");
    GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
    GLError.maybeThrowGLException("Failed to clear framebuffer", "glClear");
  }

  /** Interface to be implemented for rendering callbacks. */
  public static interface Renderer {
    /**
     * Called by {@link SampleRender} when the GL render surface is created.
     *
     * <p>See {@link GLSurfaceView.Renderer#onSurfaceCreated}.
     */
    public void onSurfaceCreated(SampleRender render);

    /**
     * Called by {@link SampleRender} when the GL render surface dimensions are changed.
     *
     * <p>See {@link GLSurfaceView.Renderer#onSurfaceChanged}.
     */
    public void onSurfaceChanged(SampleRender render, int width, int height);

    /**
     * Called by {@link SampleRender} when a GL frame is to be rendered.
     *
     * <p>See {@link GLSurfaceView.Renderer#onDrawFrame}.
     */
    void onDrawFrame(SampleRender render);

    void  onMove(float posX, float posY);

    void  onScale(float scaleFactor);

    void onRotation(float x, float y);
  }

  /* package-private */
  AssetManager getAssets() {
    return assetManager;
  }

  private void useFramebuffer(Framebuffer framebuffer) {
    int framebufferId;
    int viewportWidth;
    int viewportHeight;
    if (framebuffer == null) {
      framebufferId = 0;
      viewportWidth = this.viewportWidth;
      viewportHeight = this.viewportHeight;
    } else {
      framebufferId = framebuffer.getFramebufferId();
      viewportWidth = framebuffer.getWidth();
      viewportHeight = framebuffer.getHeight();
    }
    GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebufferId);
    GLError.maybeThrowGLException("Failed to bind framebuffer", "glBindFramebuffer");
    GLES30.glViewport(0, 0, viewportWidth, viewportHeight);
    GLError.maybeThrowGLException("Failed to set viewport dimensions", "glViewport");
  }
}
