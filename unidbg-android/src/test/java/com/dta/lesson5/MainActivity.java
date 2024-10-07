package com.dta.lesson5;

import com.github.unidbg.*;
import com.github.unidbg.Module;
import com.github.unidbg.arm.HookStatus;
import com.github.unidbg.arm.backend.DynarmicFactory;

import com.github.unidbg.arm.backend.unicorn.Unicorn;
import com.github.unidbg.hook.HookContext;
import com.github.unidbg.hook.ReplaceCallback;
import com.github.unidbg.hook.hookzz.HookZz;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import keystone.Keystone;
import keystone.KeystoneArchitecture;
import keystone.KeystoneMode;
import unicorn.ArmConst;

import javax.swing.*;
import java.io.File;

public class MainActivity {

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainActivity mainActivity = new MainActivity();
        System.out.println("load offset=" + (System.currentTimeMillis() - start) + "ms");

        //mainActivity.callAdd();
        //mainActivity.hookAdd();
        mainActivity.patch2();
        mainActivity.callAdd();
    }

    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    private MainActivity() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                .addBackendFactory(new DynarmicFactory(true))
                .build();
        Memory memory = emulator.getMemory();
        LibraryResolver resolver = new AndroidResolver(23);
        memory.setLibraryResolver(resolver);

        vm = emulator.createDalvikVM();

        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/java/com/dta/lesson5/libnative-lib.so"), true);
        module = dm.getModule();

        vm.callJNI_OnLoad(emulator, module);
    }

    private void callAdd() {
        DvmObject<?> obj = ProxyDvmObject.createObject(vm, this);
        int result = obj.callJniMethodInt(emulator,
                "add(II)I",
                30, 98);
        System.out.println("Unidbg callAdd() Result = " + result);
    }

    private void hookAdd() {
        Symbol add = module.findSymbolByName("Java_com_dta_lesson5_MainActivity_add");

        HookZz hook = HookZz.getInstance(emulator);
        hook.replace(add, new ReplaceCallback() {
        //hook.replace(module.base + 0xc31c + 1, new ReplaceCallback() {

            @Override
            public HookStatus onCall(Emulator<?> emulator, HookContext context, long originFunction) {
                //System.out.println("onCall(Emulator<?> emulator, HookContext context, long originFunction)");

                System.out.println(String.format("R2=%d, R3=%d", context.getIntArg(2), context.getIntArg(3)));
                emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_R3, 555);
                System.out.println(String.format("AfterHOOK R2=%d, R3=%d", context.getIntArg(2), context.getIntArg(3)));
                return super.onCall(emulator, context, originFunction);
            }

            @Override
            public void postCall(Emulator<?> emulator, HookContext context) {
                System.out.println("postCall(Emulator<?> emulator, HookContext context)");
                //emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_R0, 666);
                super.postCall(emulator, context);
            }

        }, true);
    }

    private void patch() {
        UnidbgPointer pointer = UnidbgPointer.pointer(emulator,module.base + 0xc328);
        byte[] code = new byte[]{(byte)0xd0, 0x1a};
        pointer.write(code);
    }

    private void patch2() {
        UnidbgPointer pointer = UnidbgPointer.pointer(emulator,module.base + 0xc328);
        Keystone keystone = new Keystone(KeystoneArchitecture.Arm, KeystoneMode.ArmThumb);
        String s = "subs r0, r2, r3";
        byte[] code = keystone.assemble(s).getMachineCode();
        pointer.write(code);
    }
}
