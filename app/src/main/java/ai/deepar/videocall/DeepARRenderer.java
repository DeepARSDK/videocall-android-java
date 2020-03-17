package ai.deepar.videocall;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.view.Surface;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;


import ai.deepar.ar.DeepAR;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.AgoraVideoFrame;

import android.opengl.Matrix;


public class DeepARRenderer implements GLSurfaceView.Renderer {

    private final String vertexShaderCode =
            "attribute vec4 vPosition;" +
                    "attribute vec2 vUv;" +
                    "varying vec2 uv; " +

                    "void main() {" +
                    "gl_Position = vPosition;" +
                    "uv = vUv;" +
                    "}";

    private final String fragmentShaderCode =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;" +
                    "varying vec2 uv; " +
                    "uniform samplerExternalOES sampler;" +
                    "void main() {" +
                    "  gl_FragColor = texture2D(sampler, uv); " +
                    "}";

    static float squareCoords[] = {
            -1.0f,  1.0f, 0.0f,
            -1.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            1.0f,  1.0f, 0.0f };

    static float uv[] = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f
    };

    private final short drawOrder[] = { 0, 1, 2, 0, 2, 3 };

    private FloatBuffer vertexBuffer;
    private FloatBuffer uvbuffer;
    private ShortBuffer drawListBuffer;
    private int program;
    private int texture;
    private SurfaceTexture surfaceTexture;
    private Surface surface;

    private DeepAR deepAR;
    private RtcEngine rtcEngine;
    private boolean updateTexImage;

    private boolean callInProgress = false;

    private EGLContext mEGLCurrentContext;

    private int textureWidth;
    private int textureHeight;

    float[] matrix = new float[16];


    public DeepARRenderer(DeepAR deepAR, RtcEngine rtcEngine) {
        this.updateTexImage = false;
        this.deepAR = deepAR;
        this.rtcEngine = rtcEngine;
    }

    public static int loadShader(int type, String shaderCode){
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);

        ByteBuffer bb = ByteBuffer.allocateDirect(squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);

        ByteBuffer bb2 = ByteBuffer.allocateDirect(squareCoords.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        uvbuffer = bb2.asFloatBuffer();
        uvbuffer.put(uv);
        uvbuffer.position(0);

        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        Matrix.setIdentityM(this.matrix, 0);

    }

    @Override
    public void onSurfaceChanged(GL10 gl, final int width, final int height) {
        GLES20.glViewport(0, 0, width, height);

        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        texture = textures[0];

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture);

        textureWidth = width;
        textureHeight = height;


        GLES20.glTexImage2D(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0, GLES20.GL_RGBA, textureWidth, textureHeight, 0,GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        surfaceTexture = new SurfaceTexture(texture);
        surface = new Surface(surfaceTexture);

        surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                updateTexImage = true;
            }
        });


        deepAR.setRenderSurface(surface, textureWidth, textureHeight);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glFinish();
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (updateTexImage) {
            updateTexImage = false;
            synchronized (this) {
                surfaceTexture.updateTexImage();
            }
        }

        surfaceTexture.getTransformMatrix(matrix);

        GLES20.glUseProgram(program);
        int positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer);

        int uvHandle = GLES20.glGetAttribLocation(program, "vUv");
        GLES20.glEnableVertexAttribArray(uvHandle);
        GLES20.glVertexAttribPointer(uvHandle, 2, GLES20.GL_FLOAT, false, 8, uvbuffer);

        int sampler = GLES20.glGetUniformLocation(program, "sampler");
        GLES20.glUniform1i(sampler, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(uvHandle);
        GLES20.glUseProgram(0);

        if (callInProgress) {
            AgoraVideoFrame frame = new AgoraVideoFrame();
            frame.textureID = texture;
            frame.height = textureHeight;
            frame.stride = textureWidth;
            frame.syncMode = true;
            frame.format = AgoraVideoFrame.FORMAT_TEXTURE_OES;
            frame.timeStamp = System.currentTimeMillis();
            frame.eglContext11 = mEGLCurrentContext;
            frame.transform = matrix;
            frame.rotation = 180;
            boolean success = rtcEngine.pushExternalVideoFrame(frame);
        }
    }

    public boolean isCallInProgress() {
        return callInProgress;
    }

    public void setCallInProgress(boolean callInProgress) {
        this.callInProgress = callInProgress;
    }

    public static class MyContextFactory implements GLSurfaceView.EGLContextFactory {

        private DeepARRenderer renderer;
        public MyContextFactory(DeepARRenderer renderer) {
            this.renderer = renderer;
        }
        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig config) {
            int[] attrib_list = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL10.EGL_NONE };
            renderer.mEGLCurrentContext =  egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, attrib_list);;
            return renderer.mEGLCurrentContext;
        }
        public void destroyContext(EGL10 egl, EGLDisplay display,
                                   EGLContext context) {
            if (!egl.eglDestroyContext(display, context)) {
            }
        }
    }

}
