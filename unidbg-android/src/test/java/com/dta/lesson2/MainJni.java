package com.dta.lesson2;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.LibraryResolver;
import com.github.unidbg.arm.backend.DynarmicFactory;

import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.Module;
import com.sun.jna.Pointer;

import java.io.File;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class MainJni extends AbstractJni {

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainJni mainJni = new MainJni();
        System.out.println("load offset=" + (System.currentTimeMillis() - start) + "ms");
        mainJni.callMd5();
    }

    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    private MainJni() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                .addBackendFactory(new DynarmicFactory(true))
                .build();

        Memory memory = emulator.getMemory();
        LibraryResolver resolver = new AndroidResolver(23);
        memory.setLibraryResolver(resolver);

        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/java/com/dta/lesson2/app-md52.apk"));

        //JNI补环境
        vm.setJni(this);

        //DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson2/liblesson2.so"), true);
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson2/liblesson2-md52.so"), true);
        module = dm.getModule();

        vm.callJNI_OnLoad(emulator, module);
    }

    private void callMd5() {
        //DvmObject<?> obj = ProxyDvmObject.createObject(vm, this);
        DvmObject obj = vm.resolveClass("com/dta/lesson2/MainActivity").newObject(null);
        String data = "dta";
        DvmObject dvmObject = obj.callJniMethodObject(emulator,
                "md52([B)Ljava/lang/String;)",
                data.getBytes());
        String result = (String) dvmObject.getValue();
        System.out.println("Unidbg callMd5() Result ==> " + result);
    }

    //继承AbstractJni,补环境
    @Override
    public void callVoidMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if(signature.equals("java/security/MessageDigest->update([B)V"))  {
            MessageDigest messageDigest = (MessageDigest) dvmObject.getValue();

            int intArg = vaList.getIntArg(0);
            Object object = vm.getObject(intArg).getValue();
            messageDigest.update((byte[]) object);
            return;
        }

        super.callVoidMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if(signature.equals("java/security/MessageDigest->digest()[B"))  {
            MessageDigest messageDigest = (MessageDigest) dvmObject.getValue();

            byte[] digest = messageDigest.digest();
            DvmObject<?> object = ProxyDvmObject.createObject(vm ,digest);
            vm.addLocalObject(object);
            return object;
        }
        if(signature.equals("com/dta/lesson2/MainActivity->byte2hex([B)Ljava/lang/String;"))  {
            MessageDigest messageDigest = (MessageDigest) dvmObject.getValue();

            int intArg = vaList.getIntArg(0);
            Object object = vm.getObject(intArg).getValue();
            String s = byte2hex((byte[]) object);
            StringObject stringObject = new StringObject(vm, s);
            vm.addLocalObject(stringObject);
            return stringObject;
        }

        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    private String byte2hex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for(byte b : data) {
            String s = Integer.toHexString(b & 0xFF);
            if(s.length() < 2){
                sb.append("0");
            }
            sb.append(s);
        }
        return sb.toString();
    }

}
