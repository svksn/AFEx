# AFEx
Audio Feature Extraction Framework for Android

## How to build

### LabStreamingLayer
External libraries are required for LabStreamingLayer:

    [https://github.com/sccn/liblsl.git]
    [https://github.com/labstreaminglayer/liblsl-Java.git]

See/set the path of liblsl-java in ```settings.gradle```. liblsl should be on the same directory level. 

### cmake

LSL requires cmake v3.12+, which doesn't seem to come with current Android SDKs. 

#### Windows 
A current version can be installed manually next to an existing copy, e.g. to ```C:\Users\<user>\AppData\Local\Android\Sdk\cmake\3.18.2```

    [https://cmake.org/download/]

You may also need Ninja. The binary can be placed into the bin folder of the cmake instalation.

    [https://github.com/ninja-build/ninja]

The new cmake path can be set in ```local.properties``` (see ```build.grade``` of the liblsl-java module).

#### Linux & Co. 
The cmake version supplied by the distribution's repositories should be recent enough and can be installed using the package manager. 

