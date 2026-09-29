
The **eaapi.jar** in this directory is Sparx EA's Java API. It is dated 7 October 2024 and, as checked on 29 September 2026, is identical to the jar shipped with EA 17.1, the latest release. It is meant to track a current EA release, so refresh it from a newer EA installation when one is available. It ships with Sparx EA in the "Java API" folder of the installation directory, similar to:

> `%WINDOWS_PROGRAM_FILES%\Sparx Systems\EAxx\Java API  (e.g. "C:\Program Files\Sparx Systems\EA17\Java API")`

During the build of **cim-compare-x.x.x.jar** this file is extracted and packaged within it. In our testing this jar works with both EA16 (64-bit) and EA15 (32-bit) installations (see `dual-installation-test-results.txt` in the repository root).

Also shipped within the above directory are the **SSJavaCOM.dll** and **SSJavaCOM64.dll** COM Modules. The purpose of each of these files is as follows:

  - **SSJavaCOM.dll** - the 32-bit COM Module DLL (Dynamic Linked Library) that the Java `eaapi.jar` library will link to when a 32-bit Java JVM/JRE is used to run `cim-compare-x.x.x.jar` from the command line. Comparing `.eap` project files needs this path: `.eap` files use Microsoft's Jet database engine, which is 32-bit only, so only 32-bit EA can open them.
  	
  - **SSJavaCOM64.dll** - the 64-bit COM Module DLL that the Java `eaapi.jar` library will link to when a 64-bit Java JVM/JRE is used to run `cim-compare-x.x.x.jar` from the command line. This is the usual path for `.qea` project files with a 64-bit EA (EA 16 or later). Note that `.qea` is not a 64-bit-only format: Sparx states that `.qea` files work in both 32-bit and 64-bit EA.
  	
  - **eaapi.jar** - the Java JNI interface library used by **cim-compare** to communicate to the appropriate COM Module DLL (32-bit or 64-bit) depending on the particular JVM (32-bit or 64-bit) being used on the command line. Which DLL to load is decided at run time from the JVM: when Java reports its architecture (`os.arch`) as `x86`, i.e. 32-bit Java, it loads `SSJavaCOM.dll`; otherwise it loads `SSJavaCOM64.dll`. The DLLs must be where Java can find them at run time. Java looks in the folders listed in `java.library.path`, which on Windows defaults to the folders on the `PATH` (Sparx's own instructions copy `SSJavaCOM.dll` into a Windows system folder). For **cim-compare** we recommend naming the folder explicitly with the `-Djava.library.path` JVM parameter, as in the examples below; on a machine with more than one EA installation it is the way to choose which installation's DLLs are used.
  	
To use **cim-compare** on a system with a dual 32-bit and 64-bit Sparx EA installation (e.g. EA 15.x and EA 16.x) you will need to have a configuration similar to the following:

```
C:\cim-compare\cim-compare-2.0.1.jar  (the latest release downloaded from https://cim-compare.ucaiug.io)
C:\cim-compare\ea15\SSJavaCOM.dll  (copied from "C:\Program Files (x86)\Sparx Systems\EA15\Java API")
C:\cim-compare\ea15\SSJavaCOM64.dll  (copied from "C:\Program Files (x86)\Sparx Systems\EA15\Java API")
C:\cim-compare\ea16\SSJavaCOM.dll  (copied from "C:\Program Files\Sparx Systems\EA16\Java API")
C:\cim-compare\ea16\SSJavaCOM64.dll  (copied from "C:\Program Files\Sparx Systems\EA16\Java API")
```

Of importance is for each installation's set of DLL files to be located in their own directory. This will allow for the ability to isolate where Java looks for its COM Modules based on release.

Following is a set of command lines based on the example configuration. The first compares `.eap` files with 32-bit Java and the 32-bit EA15 installation; the second compares `.qea` files with 64-bit Java and the 64-bit EA16 installation. Each is split over several lines with `^`, the Windows command prompt's line-continuation character.

```
"C:\Program Files (x86)\Zulu\zulu-17\bin\java.exe" -Xmx1G -Djava.library.path="C:\cim-compare\ea15" ^
  -jar cim-compare-2.0.1.jar cim17v40.eap cim18v02.eap comparison-report.html ^
  --include-diagrams --image-type=JPG
```

The above command line example uses:
 - a 32-bit Java 17 JRE/JVM  (i.e. "C:\Program Files (x86)\Zulu\zulu-17\bin\java.exe")
 - a max heap size of 1G  (i.e. 1GB specified via `-Xmx1G`)
 - the 32-bit COM DLL loaded from the `C:\cim-compare\ea15` directory  (i.e. via `-Djava.library.path="C:\cim-compare\ea15"`) 
 - cim17v40.eap as the input baseline model  (an `.eap` project file, which only 32-bit EA can open)
 - cim18v02.eap as the input destination model  (an `.eap` project file, which only 32-bit EA can open)
 - comparison-report.html as the name of the generated report
 - the inclusion of changed diagrams in the report (i.e. `--include-diagrams`)
 - JPG for the type of diagrams (i.e. `--image-type=JPG`)

```
"C:\Program Files\Zulu\zulu-17\bin\java.exe" -Xmx4G -Djava.library.path="C:\cim-compare\ea16" ^
  -jar cim-compare-2.0.1.jar cim17v40.qea cim18v02.qea comparison-report.html ^
  --include-diagrams --image-type=JPG
```

The above command line example uses:
 - a 64-bit Java 17 JRE/JVM  (i.e. "C:\Program Files\Zulu\zulu-17\bin\java.exe")
 - a max heap size of 4G  (i.e. 4GB specified via `-Xmx4G`)
 - the 64-bit COM DLL loaded from the `C:\cim-compare\ea16` directory  (i.e. via `-Djava.library.path="C:\cim-compare\ea16"`) 
 - cim17v40.qea as the input baseline model  (a `.qea` project file, opened here by the 64-bit EA16)
 - cim18v02.qea as the input destination model  (a `.qea` project file, opened here by the 64-bit EA16)
 - comparison-report.html as the name of the generated report
 - the inclusion of changed diagrams in the report (i.e. `--include-diagrams`)
 - JPG for the type of diagrams (i.e. `--image-type=JPG`)
 
Finally, if you use `.eap` project files as input into **cim-compare** then 32-bit Java (and a 32-bit EA) must be used. For `.qea` files, use Java of the same bit-width as the EA installation that will open them: 64-bit Java with a 64-bit EA (EA 16 or later), which is the combination tested above.

**Java 24 and later:** Java prints a "restricted method" warning when a program loads a native library such as `SSJavaCOM.dll` / `SSJavaCOM64.dll` ([JEP 472](https://openjdk.org/jeps/472)), and a future Java release will block it unless it is allowed. From **cim-compare 2.0.1** the jar allows it itself (`Enable-Native-Access: ALL-UNNAMED` in its manifest), so no extra setting is needed. With 2.0.0 or earlier, add `--enable-native-access=ALL-UNNAMED` before `-jar` (together with the XML settings described in the main README under [Java 24 and Later](https://github.com/cimug-org/cim-compare#java-24-and-later)). Windows builds of Java 24 and later are 64-bit only, so they apply to `.qea` input; `.eap` input needs 32-bit Java 21 or earlier.
