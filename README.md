# CameraDemo
Android Camera Demo.
Now it has one Activity that shows "How to Use GLSurfaceView for Camera Preview "
It also shows:
1. How to Flip the View 
2. How to Capture Screenshot of GLSurfaceView to bitmap

This demo is simple, some encapsulation is needed if you want to use code in this demo to build your own production app. for example, 'cameraViewContainer' in the 'CameraGLSurfaceActivity' now is just FrameLayout, you should make it a custom view like real 'CameraViewContainer' and remove methods like 'resizeSurfaceView' there.

[An article in Chinese](https://www.jianshu.com/p/c3ebb965bce6) will be written in the future to illustrate more about this Demo, stay tuned!
