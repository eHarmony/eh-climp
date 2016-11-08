CLIMP
======

openCL Image Manipulation Program

Provides opencl shaders to perform high quality, intensive operations on the GPU.

Requirements:
* Correctly installed `libjpeg-turbo 1.5.0` (http://libjpeg-turbo.virtualgl.org/)
* Set `LD_LIBRARY_PATH` to libjpeg-turbo directory
```
LD_LIBRARY_PATH=/opt/libjpeg-turbo/lib64
export LD_LIBRARY_PATH
```

Run with `java -cp climp-0.0.1-SNAPSHOT-shaded.jar com.eharmony.climp.OpenclResizer`

