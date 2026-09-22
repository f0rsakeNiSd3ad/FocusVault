package com.FocusVault.blocker;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Tlhelp32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.win32.W32APIOptions;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ProcessBlocker handles native process termination on Windows using JNA.
 * It is designed to run in a low-priority thread to guarantee zero game stutters.
 */
public class ProcessBlocker {

    private final Set<String> blacklistedProcesses;
    private final ScheduledExecutorService scheduler;
    private final java.util.prefs.Preferences prefs;
    
    private static final String PROCESSES_KEY = "FocusVault_blacklisted_processes";
    
    // Windows API access right required to terminate a process
    private static final int PROCESS_TERMINATE = 0x0001;
    
    // Define TerminateProcess as it's part of the standard Kernel32 interface we need to use
    public interface MyKernel32 extends Kernel32 {
        MyKernel32 INSTANCE = Native.load("kernel32", MyKernel32.class, W32APIOptions.DEFAULT_OPTIONS);
        boolean TerminateProcess(HANDLE hProcess, int uExitCode);
    }

    private volatile boolean isBlockingActive = false;

    public ProcessBlocker() {
        this.blacklistedProcesses = new HashSet<>();
        this.prefs = java.util.prefs.Preferences.userNodeForPackage(ProcessBlocker.class);
        loadPrefs();
        
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ProcessBlocker-Thread");
            t.setPriority(Thread.MIN_PRIORITY);
            t.setDaemon(true);
            return t;
        });
        
        // Start the ticker immediately, but it obeys the boolean flag
        this.scheduler.scheduleAtFixedRate(this::checkAndTerminateProcesses, 0, 2, TimeUnit.SECONDS);
    }

    private void loadPrefs() {
        String saved = prefs.get(PROCESSES_KEY, "");
        if (!saved.isEmpty()) {
            for (String s : saved.split(",")) {
                if (!s.trim().isEmpty()) {
                    blacklistedProcesses.add(s.trim());
                }
            }
        }
    }

    private void savePrefs() {
        prefs.put(PROCESSES_KEY, String.join(",", blacklistedProcesses));
    }

    public void addBlacklistedProcess(String processName) {
        blacklistedProcesses.add(processName.toLowerCase());
        savePrefs();
    }

    public void removeBlacklistedProcess(String processName) {
        blacklistedProcesses.remove(processName.toLowerCase());
        savePrefs();
    }

    public Set<String> getBlacklistedProcesses() {
        return new HashSet<>(blacklistedProcesses);
    }

    public void startBlocking() {
        isBlockingActive = true;
        System.out.println("ProcessBlocker active.");
    }

    public void stopBlocking() {
        isBlockingActive = false;
        System.out.println("ProcessBlocker inactive.");
    }
    
    public void shutdown() {
        scheduler.shutdown();
        System.out.println("ProcessBlocker shutdown.");
    }

    private DomainBlocker domainBlocker;

    public void setDomainBlocker(DomainBlocker domainBlocker) {
        this.domainBlocker = domainBlocker;
    }

    private void checkAndTerminateProcesses() {
        try {
            if (!isBlockingActive) return;
            
            boolean hasProcesses = !blacklistedProcesses.isEmpty();
            boolean hasDomains = domainBlocker != null && !domainBlocker.getBlacklistedDomains().isEmpty();
            
            if (!hasProcesses && !hasDomains) return;

            // 1. Terminate by .exe name (Catches all processes, including background ones)
            if (hasProcesses) {
                HANDLE snapshot = MyKernel32.INSTANCE.CreateToolhelp32Snapshot(Tlhelp32.TH32CS_SNAPPROCESS, new DWORD(0));
                if (snapshot != WinNT.INVALID_HANDLE_VALUE) {
                    try {
                        Tlhelp32.PROCESSENTRY32.ByReference processEntry = new Tlhelp32.PROCESSENTRY32.ByReference();
                        if (MyKernel32.INSTANCE.Process32First(snapshot, processEntry)) {
                            do {
                                String exeName = Native.toString(processEntry.szExeFile).toLowerCase();
                                if (blacklistedProcesses.contains(exeName)) {
                                    terminateProcess(processEntry.th32ProcessID.intValue(), exeName);
                                }
                            } while (MyKernel32.INSTANCE.Process32Next(snapshot, processEntry));
                        }
                    } finally {
                        MyKernel32.INSTANCE.CloseHandle(snapshot);
                    }
                }
            }

            // 2. Terminate by Window Title (Closes browsers if they have the blocked domain open)
            if (hasDomains) {
                Set<String> domains = domainBlocker.getBlacklistedDomains();
                java.util.List<ProcessHelper.ProcessInfo> apps = ProcessHelper.getRunningGUIApplications();
                
                for (ProcessHelper.ProcessInfo app : apps) {
                    String titleLower = app.windowTitle.toLowerCase();
                    for (String domain : domains) {
                        String baseName = extractBaseName(domain);
                        if (!baseName.isEmpty() && titleLower.contains(baseName)) {
                            System.out.println("[Blocker] Domain match found in window: '" + app.windowTitle + "'");
                            // Close only the specific tab instead of the entire browser
                            closeBrowserTab(app.hWnd);
                            break; // Move to next app
                        }
                    }
                }
            }
        } catch (Throwable t) {
            // Prevent silent thread death in ScheduledExecutorService
            t.printStackTrace();
        }
    }

    private String extractBaseName(String input) {
        String s = input.toLowerCase().trim();
        s = s.replace("http://", "").replace("https://", "").replace("www.", "");
        if (s.contains("/")) s = s.substring(0, s.indexOf("/"));
        int dot = s.lastIndexOf(".");
        if (dot > 0) s = s.substring(0, dot);
        return s;
    }

    private void closeBrowserTab(com.sun.jna.platform.win32.WinDef.HWND hWnd) {
        final int SW_RESTORE = 9;
        
        com.sun.jna.platform.win32.WinDef.HWND currentFg = com.sun.jna.platform.win32.User32.INSTANCE.GetForegroundWindow();
        boolean isForeground = false;
        
        if (currentFg != null && currentFg.equals(hWnd)) {
            isForeground = true;
        } else {
            // Force window to foreground
            com.sun.jna.platform.win32.User32.INSTANCE.ShowWindow(hWnd, SW_RESTORE);
            com.sun.jna.platform.win32.User32.INSTANCE.SetForegroundWindow(hWnd);
            try { Thread.sleep(250); } catch (Exception e) {}
            
            currentFg = com.sun.jna.platform.win32.User32.INSTANCE.GetForegroundWindow();
            
            if (currentFg != null && currentFg.equals(hWnd)) {
                isForeground = true;
            } else if (currentFg != null) {
                // Fallback: Check if the foreground window belongs to the same Process ID
                com.sun.jna.ptr.IntByReference pid1 = new com.sun.jna.ptr.IntByReference();
                com.sun.jna.platform.win32.User32.INSTANCE.GetWindowThreadProcessId(hWnd, pid1);
                
                com.sun.jna.ptr.IntByReference pid2 = new com.sun.jna.ptr.IntByReference();
                com.sun.jna.platform.win32.User32.INSTANCE.GetWindowThreadProcessId(currentFg, pid2);
                
                if (pid1.getValue() == pid2.getValue()) {
                    isForeground = true;
                }
            }
        }
        
        // Safety check to ensure we only send Ctrl+W to the offending browser
        if (isForeground) {
            try {
                java.awt.Robot robot = new java.awt.Robot();
                robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL);
                robot.keyPress(java.awt.event.KeyEvent.VK_W);
                robot.keyRelease(java.awt.event.KeyEvent.VK_W);
                robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL);
                System.out.println("[Blocker] Sent Ctrl+W to close tab.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("[Blocker] Could not safely verify foreground focus. Retrying next tick.");
        }
    }

    private void terminateProcess(int pid, String processName) {
        HANDLE hProcess = MyKernel32.INSTANCE.OpenProcess(PROCESS_TERMINATE, false, pid);
        if (hProcess != null) {
            boolean success = MyKernel32.INSTANCE.TerminateProcess(hProcess, 1);
            MyKernel32.INSTANCE.CloseHandle(hProcess);
            if (success) {
                System.out.println("[Blocker] Terminated blocked process: " + processName + " (PID: " + pid + ")");
            }
        }
    }
}
