package com.FocusVault.blocker;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProcessHelper {

    public static class ProcessInfo {
        public String windowTitle;
        public String exeName;
        public int pid;
        public com.sun.jna.platform.win32.WinDef.HWND hWnd;

        public ProcessInfo(String windowTitle, String exeName, int pid, com.sun.jna.platform.win32.WinDef.HWND hWnd) {
            this.windowTitle = windowTitle;
            this.exeName = exeName;
            this.pid = pid;
            this.hWnd = hWnd;
        }

        @Override
        public String toString() {
            return windowTitle.isEmpty() ? exeName : windowTitle + " (" + exeName + ")";
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ProcessInfo that = (ProcessInfo) obj;
            return exeName.equals(that.exeName); // Distinct by executable for blocking lists
        }

        @Override
        public int hashCode() {
            return exeName.hashCode();
        }
    }

    /**
     * Finds currently running GUI applications to allow seamless user selection for blocking.
     */
    public static List<ProcessInfo> getRunningGUIApplications() {
        Set<ProcessInfo> apps = new HashSet<>();
        
        User32.INSTANCE.EnumWindows((hWnd, arg1) -> {
            if (User32.INSTANCE.IsWindowVisible(hWnd)) {
                char[] windowText = new char[512];
                User32.INSTANCE.GetWindowText(hWnd, windowText, 512);
                String wText = Native.toString(windowText).trim();
                
                // We only care about windows with titles (actual apps)
                if (!wText.isEmpty()) {
                    IntByReference pid = new IntByReference();
                    User32.INSTANCE.GetWindowThreadProcessId(hWnd, pid);
                    
                    String exeName = "unknown.exe";
                    WinNT.HANDLE process = Kernel32.INSTANCE.OpenProcess(
                            0x1000, // PROCESS_QUERY_LIMITED_INFORMATION
                            false, 
                            pid.getValue());
                            
                    if (process != null) {
                        char[] exePath = new char[1024];
                        IntByReference size = new IntByReference(1024);
                        
                        boolean success = Kernel32.INSTANCE.QueryFullProcessImageName(process, 0, exePath, size);
                        Kernel32.INSTANCE.CloseHandle(process);
                        
                        if (success) {
                            String fullPath = Native.toString(exePath);
                            exeName = fullPath.substring(fullPath.lastIndexOf('\\') + 1).toLowerCase();
                        }
                    }
                    apps.add(new ProcessInfo(wText, exeName, pid.getValue(), hWnd));
                }
            }
            return true;
        }, null);
        
        List<ProcessInfo> list = new ArrayList<>(apps);
        list.sort((a, b) -> a.exeName.compareToIgnoreCase(b.exeName));
        return list;
    }
}
