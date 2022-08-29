# JOVP
Open Java Engine for Visual Psychophysics

## Description
[JOVP](https://github.com/imarinfr/JOVP/) is a cross-platform engine designed for the development of software for visual psychophysics with
[Vulkan](https://www.vulkan.org/).

The software takes the perspective of an observer sitting at a specific distance from the display. Thus, field of view,
and sizes are always specified in degrees of visual angle for that observer.

The specs of the display monitors (maximum resolution in pixels, display size in mm, pixel size, and color depth)
are calculated automatically by querying the hardware and used to compute the degrees of visual angle. Some hardware
provides erroneous information on the actual physical size of the display. For these cases, it is possible to specify
the correct physical size of the display manually. 

Visual Psychophysics are best performed in full screen, but for developing, testing, and debugging purposes, the
software can also be used in window mode.

There are two basic display modes: single- and split-screen display. In the split-screen display, the left half of the
display is used to render stimulus for the left viewMode and the right half for the right viewMode. This mode can be used for
stereoptic 3D vision.

Future releases will incorporate the distortion effects of optical lenses used by the observer and interactions with
viewMode-tracking devices.

## Dependencies
* [Vulkan](https://www.vulkan.org/) - A low-overhead, cross-platform API, open standard for 3D graphics and computing.
* [LWJGL](https://www.lwjgl.org/) - Lightweight Java Game Library
* [JOLM](https://github.com/JOML-CI/JOML/) - Java OpenGL Math Library
  
## Licence and Copyright
The [JOVP](https://github.com/imarinfr/jovp) project is COPYRIGHTED by Marín-Franch I (https://optocom.es), and is
distributed under the Apache 2.0 license. Please read the license information in the attached file.

## More information
The Vulkan implementation in [JOVP](https://github.com/imarinfr/JOVP/) draws heavily from the following:
* [Vulkan Tutorial](https://vulkan-tutorial.com/) by Alexander Overvoorde
* [Vulkan-Tutorial-Java](https://github.com/Naitsirc98/Vulkan-Tutorial-Java/) - Java port of the previous C++ tutorial
* [lwjgl3-demos](https://github.com/LWJGL/lwjgl3-demos/) - A set of demos for Vulkan use with LWJGL3
