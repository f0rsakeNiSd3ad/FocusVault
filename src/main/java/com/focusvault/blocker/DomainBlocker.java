package com.FocusVault.blocker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DomainBlocker {
    private static final Path HOSTS_FILE_PATH = Paths.get(System.getenv("WINDIR"), "System32", "drivers", "etc", "hosts");
    private static final String START_MARKER = "# --- FocusVault BLOCK START ---";
    private static final String END_MARKER = "# --- FocusVault BLOCK END ---";
    
    private final Set<String> blacklistedDomains;
    private final java.util.prefs.Preferences prefs;
    private static final String DOMAINS_KEY = "FocusVault_blacklisted_domains";

    public DomainBlocker() {
        this.blacklistedDomains = new HashSet<>();
        this.prefs = java.util.prefs.Preferences.userNodeForPackage(DomainBlocker.class);
        loadPrefs();
    }

    private void loadPrefs() {
        String saved = prefs.get(DOMAINS_KEY, "");
        if (!saved.isEmpty()) {
            for (String s : saved.split(",")) {
                if (!s.trim().isEmpty()) {
                    blacklistedDomains.add(s.trim());
                }
            }
        }
    }

    private void savePrefs() {
        prefs.put(DOMAINS_KEY, String.join(",", blacklistedDomains));
    }

    public void addDomain(String domain) {
        blacklistedDomains.add(domain.toLowerCase());
        savePrefs();
    }

    public void removeDomain(String domain) {
        blacklistedDomains.remove(domain.toLowerCase());
        savePrefs();
    }

    public Set<String> getBlacklistedDomains() {
        return new HashSet<>(blacklistedDomains);
    }

    public void applyBlocks() {
        if (blacklistedDomains.isEmpty()) {
            removeBlocks();
            return;
        }

        try {
            List<String> currentLines = Files.exists(HOSTS_FILE_PATH) ? Files.readAllLines(HOSTS_FILE_PATH) : new ArrayList<>();
            List<String> newLines = new ArrayList<>();
            
            boolean inBlock = false;
            for (String line : currentLines) {
                if (line.trim().equals(START_MARKER)) {
                    inBlock = true;
                    continue;
                }
                if (line.trim().equals(END_MARKER)) {
                    inBlock = false;
                    continue;
                }
                if (!inBlock) {
                    newLines.add(line);
                }
            }

            newLines.add(START_MARKER);
            for (String domain : blacklistedDomains) {
                newLines.add("127.0.0.1 " + domain);
                newLines.add("127.0.0.1 www." + domain);
            }
            newLines.add(END_MARKER);

            Files.write(HOSTS_FILE_PATH, newLines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            flushDNS();
            System.out.println("Domains blocked via hosts file.");
        } catch (IOException e) {
            System.err.println("Failed to write to hosts file. Ensure app is running as Administrator. " + e.getMessage());
        }
    }

    public void removeBlocks() {
        try {
            if (!Files.exists(HOSTS_FILE_PATH)) return;

            List<String> currentLines = Files.readAllLines(HOSTS_FILE_PATH);
            List<String> newLines = new ArrayList<>();
            
            boolean inBlock = false;
            for (String line : currentLines) {
                if (line.trim().equals(START_MARKER)) {
                    inBlock = true;
                    continue;
                }
                if (line.trim().equals(END_MARKER)) {
                    inBlock = false;
                    continue;
                }
                if (!inBlock) {
                    newLines.add(line);
                }
            }

            // Only write to file and flush DNS if we actually removed something
            if (currentLines.size() != newLines.size()) {
                Files.write(HOSTS_FILE_PATH, newLines, StandardOpenOption.TRUNCATE_EXISTING);
                flushDNS();
                System.out.println("Domain blocks removed.");
            }
        } catch (IOException e) {
            System.err.println("Failed to write to hosts file. Ensure app is running as Administrator. " + e.getMessage());
        }
    }

    private void flushDNS() {
        try {
            Runtime.getRuntime().exec("ipconfig /flushdns");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
