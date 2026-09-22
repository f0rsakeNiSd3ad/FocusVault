package com.FocusVault.anticheat;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import java.util.Arrays;
import java.util.List;

/**
 * Uses Windows User32 API to detect system-wide keyboard and mouse idle time.
 */
public class PresenceDetector {

    public interface User32 extends StdCallLibrary {
        User32 INSTANCE = Native.load("user32", User32.class, W32APIOptions.DEFAULT_OPTIONS);

        boolean GetLastInputInfo(LASTINPUTINFO result);
        
        public static class LASTINPUTINFO extends Structure {
            public int cbSize = 8;
            public int dwTime;

            @Override
            protected List<String> getFieldOrder() {
                return Arrays.asList("cbSize", "dwTime");
            }
        }
    }
    
    public interface Kernel32 extends StdCallLibrary {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class, W32APIOptions.DEFAULT_OPTIONS);
        int GetTickCount();
    }

    /**
     * Gets the idle time in milliseconds.
     */
    public static long getIdleTimeMillis() {
        User32.LASTINPUTINFO lastInputInfo = new User32.LASTINPUTINFO();
        if (User32.INSTANCE.GetLastInputInfo(lastInputInfo)) {
            long tickCount = Kernel32.INSTANCE.GetTickCount() & 0xFFFFFFFFL; // handle unsigned 32-bit
            long lastInputTime = lastInputInfo.dwTime & 0xFFFFFFFFL;
            return tickCount - lastInputTime;
        }
        return 0;
    }
}
