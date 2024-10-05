package com.dta.lesson2;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.LibraryResolver;
import com.github.unidbg.arm.backend.DynarmicFactory;

import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.StringObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.Module;
import com.sun.jna.Pointer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class MainActivity {

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainActivity mainActivity = new MainActivity();
        System.out.println("load offset=" + (System.currentTimeMillis() - start) + "ms");
        mainActivity.callMd5();
        mainActivity.call_address();
    }

    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    private MainActivity() {
        emulator = AndroidEmulatorBuilder
                //.for64Bit()
                .for32Bit()
                .addBackendFactory(new DynarmicFactory(true))
                .build();
        Memory memory = emulator.getMemory();
        LibraryResolver resolver = new AndroidResolver(23);
        memory.setLibraryResolver(resolver);

        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/java/com/dta/lesson2/app-debug.apk"));

        //DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson2/liblesson2.so"), true);
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson2/liblesson2-v7a.so"), true);
        module = dm.getModule();

        vm.callJNI_OnLoad(emulator, module);
    }

    private void callMd5() {
        DvmObject<?> obj = ProxyDvmObject.createObject(vm, this);
        String data = "dta";
        DvmObject dvmObject = obj.callJniMethodObject(emulator,
                "md5(Ljava/lang/String;)Ljava/lang/String;)",
                data);
        String result = (String) dvmObject.getValue();
        System.out.println("Unidbg callMd5() Result: " + result);
    }

    private void call_address() {
        Pointer jniEnv = vm.getJNIEnv();
        DvmObject<?> obj = ProxyDvmObject.createObject(vm, this);
        StringObject data = new StringObject(vm, "dta");

        List<Object> args = new ArrayList<>();
        args.add(jniEnv);
        args.add(vm.addGlobalObject(obj));
        args.add(vm.addGlobalObject(data));

        //Number number = module.callFunction(emulator, 0x249d8, args.toArray());
        Number number = module.callFunction(emulator, 0x128bc + 1, args.toArray());

        DvmObject dvmObject = vm.getObject(number.intValue());
        String value = (String) dvmObject.getValue();
        System.out.println("Unidbg call_address() Result: " + value);
    }
}

