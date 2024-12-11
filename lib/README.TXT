
As of cim-compare 1.3.0 we now have multiple eaapi.jar files, one per release of Sparx EA. This is necessary due to the new packaging
and distribution approach to support both 32-bit .eap and 64-bit .qea files.

The eaapi.jar is typically located in the Sparx install directory at:

%WINDOWS_PROGRAM_FILES%\Sparx Systems\EAxx\Java API  (e.g. "C:\Program Files\Sparx Systems\EA16\Java API")

To utilize cim-compare to process Enterprise Architect project files (.eap, .eapx, .qea, .qeax, etc.) directly, the below
step is also required depending on the version of Java being used. Note that if using 64-bit machine, it is recommended 
to always use 64-bit Java:

    64-bit Java:
        Copy the file SSJavaCOM64.dll (the 64-bit DLL) located in <EA installation folder>\Java API to your <Windows folder>\System32 
        (on a 64-bit machine).
        
    32-bit Java:
        Copy the file SSJavaCom.dll (the 32 bit DLL) located in <EA installation folder>\Java API to your <Windows folder>\System32 
        (on a 32-bit machine) or to <Windows folder>/SysWOW64 (on a 64-bit machine).

If the possibility exists that cim-compare.jar will be executed using either a 32-bit or 64-bit Java virtual machines than it 
may be prudent to copy both .dll(s) to ensuring both are accessible by the Java JRE/JVM.

NOTE: If cim-compare will not be used to process EA .eap or .qea files then the above steps are not necessary.

