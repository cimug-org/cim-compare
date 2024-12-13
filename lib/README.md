
The **eaapi.jar** is this directory is intended to always be that from the latest stable release of Sparx EA which, as of October 2024, is currently EA17. This file ships with Sparx EA and is typically delivered in an installation directory similar to:

> `%WINDOWS_PROGRAM_FILES%\Sparx Systems\EAxx\Java API  (e.g. "C:\Program Files\Sparx Systems\EA17\Java API")`

Sparx has historically ensured backwards compatibility of the **eaapi.jar** across releases. During build of the **cim-cimpare-x.x.x.jar** this file is extracted and packaged within it. Testing has confirmed that it does successfully interface with both EA16 (64-bit) and EA15 (32-bit) installations.

Also shipped within the above directory are the **SSJavaCOM.dll** and **SSJavaCOM64.dll** COM Modules. The purpose of each of these files is as follows:

  - **SSJavaCOM.dll** - the 32-bit COM Module DLL (Dynamic Linked Library) that the Java `eaapi.jar` library will link to when a 32-bit Java JVM/JRE is used to run `cim-compare-x.x.x.jar` from the command line (i.e. required when processing the 32-bit .EAP project files).
  	
  - **SSJavaCOM64.dll** - the 64-bit COM Module DLL that the Java `eaapi.jar` library will link to when a 32-bit Java JVM/JRE is used to run `cim-compare.jar` from the command line (i.e. required when processing the 32-bit .EAP project files)
  	
  - **eaapi.jar** - the Java JNI interface library used by **cim-compare** to communicate to the appropriate COM Module DLL (32-bit or 64-bit) depending on the particular JVM (32-bit or 64-bit) being used on the command line. Which of the DLL COM Modules to load and link to is determined at run time based on the JVM. A key requirement is that the above two DLLs are available to Java at runtime. This is done on the command line by specifying their directory location using the `-Djava.library.path` JVM parameter.
  	
To use **cim-compare** on a system with a dual 32-bit and 64-bit Sparx EA installation (e.g. EA 15.x and EA 17.x) you will need to have a configuration similar to the following:

```
C:\cim-compare\cim-compare-1.3.0.jar  (the latest release downloaded from https://cim-compare.ucaiug.io)
C:\cim-compare\ea15\SSJavaCOM.dll  (copied from "C:\Program Files (x86)\Sparx Systems\EA15\Java API")
C:\cim-compare\ea15\SSJavaCOM16.dll  (copied from "C:\Program Files (x86)\Sparx Systems\EA15\Java API")
C:\cim-compare\ea16\SSJavaCOM.dll  (copied from "C:\Program Files\Sparx Systems\EA16\Java API")
C:\cim-compare\ea16\SSJavaCOM16.dll  (copied from "C:\Program Files\Sparx Systems\EA16\Java API")
```

Of importance is for each installation's set of DLL files to be located in their own directory. This will allow for the ability to isolate where Java looks for its COM Modules based on release.

Following is a set of command lines based on the example configuration. The first illustrates comparison report generation for 32-bit `.eap` files and the second for 64-bit `.qea` files.

```
"C:\Program Files (x86)\Zulu\zulu-17\bin\java.exe" -mx1024m -Djava.library.path="D:\cim-compare\ea15" -jar cim-compare-1.3.0.jar cim17v40.eap cim18v02.eap comparison-report.html --include-diagrams --image-type=JPG --minimal
```

The above command line example uses:
 - a 32-bit Java 17 JRE/JVM  (i.e. "C:\Program Files (x86)\Zulu\zulu-17\bin\java.exe")
 - a max heap size of 1024m  (i.e. 1GB specified via -mx1024m)
 - the 32-bit COM DLL loaded from the "D:\cim-compare\ea16" directory  (i.e. via -Djava.library.path="D:\cim-compare\ea16") 
 - cim17v40.eap as the input baseline model  (i.e. a 32-bit EA project file indicated by the .eap extension)
 - cim18v02.eap as the input destination model  (i.e. a 32-bit EA project file indicated by the .eap extension)
 - comparison-report.html as the name of the generated report
 - the inclusion of changed diagrams in the report (i.e. --include-diagrams)
 - JPG for the type of diagrams (i.e. --image-type=JPG)
 - the inclusion of only changed elements (i.e. --minimal)

```
"C:\Program Files\Zulu\zulu-17\bin\java.exe" -mx4096m -Djava.library.path="D:\cim-compare\ea16" -jar cim-compare-1.3.0.jar cim17v40.qea cim18v02.qea comparison-report.html --include-diagrams --image-type=JPG --minimal
```

The above command line example uses:
 - a 64-bit Java 17 JRE/JVM  (i.e. "C:\Program Files\Zulu\zulu-17\bin\java.exe")
 - a max heap size of 4096m  (i.e. 4GB specified via -mx4096m)
 - the 64-bit COM DLL loaded from the "D:\cim-compare\ea16" directory  (i.e. via -Djava.library.path="D:\cim-compare\ea16") 
 - cim17v40.qea as the input baseline model  (i.e. a 64-bit EA project file indicated by the .qea extension)
 - cim18v02.qea as the input destination model  (i.e. a 64-bit EA project file indicated by the .qea extension)
 - comparison-report.html as the name of the generated report
 - the inclusion of changed diagrams in the report (i.e. --include-diagrams)
 - JPG for the type of diagrams (i.e. --image-type=JPG)
 - the inclusion of only changed elements (i.e. --minimal)
 
Finally, it should be noted that if choosing to use `.eap` project files as input into **cim-compare** then 32-bit Java must be used.  Likewise, when using `.qea` files then 64-bit Java must be used.
