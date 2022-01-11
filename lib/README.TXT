
The eaapi.jar file in this folder currently is that shipped with the Sparx Enterprise Architect 15.2 release.  It is located in the Sparx install directory:

%WINDOWS_PROGRAM_FILES%\Sparx Systems\EA\Java API



To utilize cim-compare to process Enterprise Architect models (.eap files) the following step(s) are also required depending and the version of Java being used. On a 64-bit machine, then it is recommended to use 64-bit Java.

    64-bit Java:
        Copy the file SSJavaCOM64.dll located in <EA installation folder>/Java API to <Windows folder>/System32 (on a 64-bit machine).
        
    32-bit Java:
        Copy the file SSJavaCom.dll located in <EA installation folder>/Java API to <Windows folder>/System32 (on a 32-bit machine) or to <Windows folder>/SysWOW64 (on a 64-bit machine).

If the possibility exists that cim-compare.jar will be executed using either a 32-bit or 64-bit Java virtual machines than it may be prudent to copy both .dll(s) to ensuring both can run.



NOTE: If cim-compare will not be used to process EA .eap files then the above steps are not necessary.

