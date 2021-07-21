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

package com.google.ar.core.examples.java.ml.render

import com.google.ar.core.examples.java.common.samplerender.*
import com.google.ar.core.examples.java.common.samplerender.Mesh.createFromAsset


/**
 * Draws a label. See [draw].
 */
class ObjRender(
  val objFilename: String,
  val textureFilename: String,
  val size: Float) {

  val cache = TextTextureCache()
  lateinit var mesh: Mesh
  lateinit var texture: Texture
  lateinit var shader: Shader

  fun onSurfaceCreated(render: SampleRender) {
    shader = Shader.createFromAssets(render, "shaders/obj.vert", "shaders/obj.frag", null)
      .setBlend(
        Shader.BlendFactor.ONE, // ALPHA (src)
        Shader.BlendFactor.ONE_MINUS_SRC_ALPHA // ALPHA (dest)
      )
      .setDepthTest(true)
      .setDepthWrite(true)

    mesh = createFromAsset(render, objFilename)
    mesh.scale = size
    texture = Texture.createFromAsset(
      render,
      textureFilename,
      Texture.WrapMode.CLAMP_TO_EDGE,
      Texture.ColorFormat.LINEAR)
  }

  /**
   * Draws a label quad with text [label] at [pose]. The label will rotate to face [cameraPose] around the Y-axis.
   */
  fun draw(
    render: SampleRender,
    modelViewProjectionMatrix: FloatArray
  ) {
    shader
      .setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
      .setTexture("uTexture", texture)
    render.draw(mesh, shader)
  }
}