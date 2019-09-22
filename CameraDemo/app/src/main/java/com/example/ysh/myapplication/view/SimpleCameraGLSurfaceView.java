package com.example.ysh.myapplication.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.example.ysh.myapplication.util.CameraEngine;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES30.glUniformMatrix4fv;


/**
 * Created by hjm1fb
 * GPU占用率较低
 */

public class SimpleCameraGLSurfaceView extends BaseCameraGLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "CameraGLSurfaceView";

    private SurfaceTexture mSurfaceTexture;
    private int mTextureName;
    private RenderTool renderTool;
    private CameraEngine cameraEngine;
    private boolean bIsPreviewStarted;
    private final Queue<Runnable> mRunOnDrawEnd = new LinkedList<>();
    private float[] transformMatrix = new float[16];

    public boolean isFlipX() {
        return flipX;
    }

    private boolean flipX = false;

    public SimpleCameraGLSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(3);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //设置背景的颜色
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        renderTool = new RenderTool();

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES30.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
//        System.out.println("onDrawFrame");
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);

        if (!bIsPreviewStarted) {
            bIsPreviewStarted = initSurfaceTexture();
            return;
        }
        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(transformMatrix);
        renderTool.draw();
        runAll(mRunOnDrawEnd);
    }

    public boolean initSurfaceTexture() {
        if (cameraEngine == null) {
            Log.i(TAG, "cameraEngine or mGLSurfaceView is null!");
            return false;
        }
        if (mSurfaceTexture == null){
            int[] textures = new int[1];
            GLES30.glGenTextures(1, textures, 0);
            mTextureName = textures[0];
            mSurfaceTexture = new SurfaceTexture(mTextureName);
            mSurfaceTexture.setOnFrameAvailableListener(this);
        }
        return cameraEngine.startPreview(mSurfaceTexture);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();
    }

    @Override
    public void init(CameraEngine cameraEngine, boolean isPreviewStarted, Context context) {
        this.cameraEngine = cameraEngine;
        bIsPreviewStarted = isPreviewStarted;
    }


    public class RenderTool {
      private float shapeCoords[] = {
                -1, 1, 0,  // top left
                 1, 1, 0,   // top right
                 1, -1, 0,  // bottom right
                -1, -1, 0, // bottom left

         };

        private float textureCoords[] = {
                0, 1,  // top left
                1, 1,  // top right
                1, 0,  // bottom right
                0, 0,  // bottom left*//*


         };

        private float textureCoordsFlipX[] = {
                1, 1,  // top right
                0, 1,  // top left
                0, 0,  // bottom left
                1, 0,  // bottom right
        };



        private final String vertexShaderCode =
                "attribute vec4 aPosition;\n" +
                        "attribute vec4 aTextureCoord;\n" +
                        "varying vec2 vTextureCoord;\n" +
                        "uniform mat4 uTextureMatrix;\n" +
                        "void main() {\n" +
                        "gl_Position = aPosition;\n" +
                        "vTextureCoord = (uTextureMatrix * aTextureCoord).xy;\n" +
                        "}\n";
        private final String fragmentShaderCode =
                "#extension GL_OES_EGL_image_external : require\n" +
                        "precision mediump float;\n" +
                        "varying vec2 vTextureCoord;\n" +
                        "uniform samplerExternalOES sTexture;\n" +
                        "void main() {\n" +
                        "gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                        "}\n";
        private final int COORDS_PER_VERTEX = 3;
        private final int TEXTURE_COORS_PER_VERTEX = 2;
        private short drawOrder[] = {0, 1, 2, 0, 2, 3};

        /*Vertex buffers*/
        private final int mProgram;
        final int[] vboVertex = new int[1];
        final int[] vboTexCoord = new int[1];
        final int[] vboTexCoordFlipX = new int[1];
        final int[] vboDrawList = new int[1];
        final int[] vao = new int[1];
        final int[] vaoFlipX = new int[1];

        int positionHandler;
        int texCoordHandler;

        FloatBuffer mVertexBuffer;
        FloatBuffer mTexCoordBuffer;
        FloatBuffer mTexCoordBufferFlipX;
        ShortBuffer mDrawListBuffer;

        public RenderTool() {
            //Link Shader
            //Load Shader source code (in string)
            int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER,
                    vertexShaderCode);
            int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER,
                    fragmentShaderCode);
            mProgram = GLES30.glCreateProgram();
            GLES30.glAttachShader(mProgram, vertexShader);
            GLES30.glAttachShader(mProgram, fragmentShader);
            GLES30.glLinkProgram(mProgram);
            positionHandler = GLES30.glGetAttribLocation(mProgram, "aPosition");
            texCoordHandler = GLES30.glGetAttribLocation(mProgram, "aTextureCoord");

            ByteBuffer bb = ByteBuffer.allocateDirect(4 * shapeCoords.length);
            bb.order(ByteOrder.nativeOrder());
            mVertexBuffer = bb.asFloatBuffer();
            mVertexBuffer.put(shapeCoords);
            mVertexBuffer.position(0);

            /*Vertex texture coord buffer*/
            ByteBuffer txeb = ByteBuffer.allocateDirect(4 * textureCoords.length);
            txeb.order(ByteOrder.nativeOrder());
            mTexCoordBuffer = txeb.asFloatBuffer();
            mTexCoordBuffer.put(textureCoords);
            mTexCoordBuffer.position(0);

            ByteBuffer txebFlipX = ByteBuffer.allocateDirect(4 * textureCoordsFlipX.length);
            txebFlipX.order(ByteOrder.nativeOrder());
            mTexCoordBufferFlipX = txebFlipX.asFloatBuffer();
            mTexCoordBufferFlipX.put(textureCoordsFlipX);
            mTexCoordBufferFlipX.position(0);

		    /*Draw list buffer*/
            ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
            dlb.order(ByteOrder.nativeOrder());
            mDrawListBuffer = dlb.asShortBuffer();
            mDrawListBuffer.put(drawOrder);
            mDrawListBuffer.position(0);

            GLES30.glGenVertexArrays(1, vao, 0);
            GLES30.glBindVertexArray(vao[0]);

            createVao(false);
            GLES30.glGenVertexArrays(1, vaoFlipX, 0);
            GLES30.glBindVertexArray(vaoFlipX[0]);
            createVao(true);

        }

        private void createVao(boolean flipX) {
            GLES30.glEnableVertexAttribArray(positionHandler);

            GLES30.glEnableVertexAttribArray(texCoordHandler);

            GLES30.glGenBuffers(1, vboVertex, 0);
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboVertex[0]);
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, mVertexBuffer.capacity()
                    * 4, mVertexBuffer, GLES30.GL_STATIC_DRAW);
            GLES30.glVertexAttribPointer(positionHandler, COORDS_PER_VERTEX,
                    GLES30.GL_FLOAT, false, 0, 0);
            GLES30.glEnableVertexAttribArray(positionHandler);

            /*Vertex texture coord buffer*/
            if (flipX){
                GLES30.glGenBuffers(1, vboTexCoordFlipX, 0);
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboTexCoordFlipX[0]);
                GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, mTexCoordBufferFlipX.capacity()
                        * 4, mTexCoordBufferFlipX, GLES30.GL_STATIC_DRAW);
            }else {
                GLES30.glGenBuffers(1, vboTexCoord, 0);
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboTexCoord[0]);
                GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, mTexCoordBuffer.capacity()
                        * 4, mTexCoordBuffer, GLES30.GL_STATIC_DRAW);
            }

            GLES30.glVertexAttribPointer(texCoordHandler, TEXTURE_COORS_PER_VERTEX,
                    GLES30.GL_FLOAT, false,
                    0, 0);

            GLES30.glGenBuffers(1, vboDrawList, 0);
            GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, vboDrawList[0]);
            GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, mDrawListBuffer.capacity()
                    * 2, mDrawListBuffer, GLES30.GL_STATIC_DRAW);
        }

        public int loadShader(int type, String shaderCode) {

            // create a vertex shader type (GLES30.GL_VERTEX_SHADER)
            // or a fragment shader type (GLES30.GL_FRAGMENT_SHADER)
            int shader = GLES30.glCreateShader(type);

            // add the source code to the shader and compile it
            GLES30.glShaderSource(shader, shaderCode);
            GLES30.glCompileShader(shader);

            return shader;
        }

        private void draw() {
            GLES30.glUseProgram(mProgram);
           if (flipX){
                GLES30.glBindVertexArray(vaoFlipX[0]);
            }else {
               GLES30.glBindVertexArray(vao[0]);
           }
            int textureHandler = GLES30.glGetUniformLocation(mProgram, "sTexture");
            int textureMatrixLocation = GLES30.glGetUniformLocation(mProgram, "uTextureMatrix");

            GLES30.glUniform1i(textureHandler, 0);

            glUniformMatrix4fv(textureMatrixLocation, 1, false, transformMatrix, 0);

            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureName);

            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);

            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

            GLES30.glDrawElements(GLES30.GL_TRIANGLES, drawOrder.length, GLES30.GL_UNSIGNED_SHORT, 0);
        }

        public void updateVao() {

        }
    }

    @Override
    public Bitmap capture(final int parentWidth, final int parentHeight) throws InterruptedException {
        final Semaphore waiter = new Semaphore(0);

        final int selfWidth = getMeasuredWidth();
        final int selfHeight = getMeasuredHeight();

        // Take picture on OpenGL thread
        final int[] pixelMirroredArray = new int[parentWidth * parentHeight];
        runOnDrawEnd(new Runnable() {
            @Override
            public void run() {
                final IntBuffer pixelBuffer = IntBuffer.allocate(parentWidth * parentHeight);
                GLES30.glReadPixels((selfWidth -parentWidth)/2, (selfHeight -parentHeight)/2, parentWidth, parentHeight, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, pixelBuffer);
                int[] pixelArray = pixelBuffer.array();

                // Convert upside down mirror-reversed image to right-side up normal image.
                for (int i = 0; i < parentHeight; i++) {
                    for (int j = 0; j < parentWidth; j++) {
                        pixelMirroredArray[(parentHeight - i - 1) * parentWidth + j] = pixelArray[i * parentWidth + j];
                    }
                }
                waiter.release();
            }
        });
        requestRender();
        waiter.acquire();

        Bitmap bitmap = Bitmap.createBitmap(parentWidth, parentHeight, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(IntBuffer.wrap(pixelMirroredArray));
        return bitmap;
    }

    @Override
    public void setFlipX(boolean flipX) {
       this.flipX = flipX;
       renderTool.updateVao();
    }

    protected void runOnDrawEnd(final Runnable runnable) {
        synchronized (mRunOnDrawEnd) {
            mRunOnDrawEnd.add(runnable);
        }
    }

    private void runAll(Queue<Runnable> queue) {
        synchronized (queue) {
            while (!queue.isEmpty()) {
                queue.poll().run();
            }
        }
    }

}
